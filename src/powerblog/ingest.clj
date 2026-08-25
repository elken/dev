(ns powerblog.ingest
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [datomic.api :as d]
   [etaoin.api :as etaoin]
   [powerblog.pages.opengraph :as opengraph])
  (:import
   [java.io ByteArrayInputStream OutputStream]
   [java.math BigInteger]
   [java.nio.charset StandardCharsets]
   [java.security DigestInputStream MessageDigest]))

(defn get-page-kind [file-name]
  (cond
    (re-find #"^posts/" file-name)
    :page.kind/blog-post

    (re-find #"^index(-nb)?\.md" file-name)
    :page.kind/frontpage

    (re-find #"^about\.md" file-name)
    :page.kind/about-page

    (re-find #"\.md$" file-name)
    :page.kind/article))

(defn create-tx [file-name txes]
  (let [kind (get-page-kind file-name)]
    (for [tx txes]
      (cond-> tx
        (and (:page/uri tx) kind)
        (assoc :page/kind kind)))))

(defn- sha-256 [value]
  (let [digest (MessageDigest/getInstance "SHA-256")
        input-stream (if (string? value)
                       (ByteArrayInputStream. (.getBytes value StandardCharsets/UTF_8))
                       (io/input-stream value))]
    (with-open [stream (DigestInputStream. input-stream digest)
                sink (OutputStream/nullOutputStream)]
      (.transferTo stream sink))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn- image-digest [file]
  (when (.exists file)
    (sha-256 file)))

(defn- opengraph-cache-file []
  (io/file "resources" ".opengraph-cache.edn"))

(defn- read-opengraph-cache [file]
  (if-not (.exists file)
    {}
    (try
      (edn/read-string (slurp file))
      (catch Exception _
        {}))))

(defn- write-opengraph-cache! [file cache]
  (io/make-parents file)
  (spit file (pr-str cache)))

(defn- opengraph-card [powerpack-app resource-dir resource-path post]
  (let [post-name (last (str/split (:page/uri post) #"/"))
        file-path (io/file resource-dir resource-path "opengraph" (str post-name ".png"))
        html (opengraph/render-opengraph-page powerpack-app post)]
    {:post-name post-name
     :file-path file-path
     :html html
     :source-digest (sha-256 html)
     :image-digest (image-digest file-path)}))

(defn- regenerate-opengraph? [cache {:keys [post-name source-digest image-digest]}]
  (let [card {:source-digest source-digest
              :image-digest image-digest}]
    (or (nil? image-digest)
        (and (contains? cache post-name)
             (not= card (get cache post-name))))))

(defn- cache-entry [{:keys [source-digest file-path]}]
  {:source-digest source-digest
   :image-digest (image-digest file-path)})

(defn- screenshot-opengraph! [driver {:keys [file-path html post-name]}]
  (let [html-path (java.io.File/createTempFile post-name ".html")]
    (try
      (spit html-path html)
      (etaoin/go driver (str "file://" html-path))
      ;; Send the Chrome-specific request for a transparent background.
      (etaoin/execute {:driver driver
                       :method :post
                       :path [:session (:session driver) "chromium" "send_command_and_get_result"]
                       :data {:cmd "Emulation.setDefaultBackgroundColorOverride"
                              :params {:color {:r 0 :g 0 :b 0 :a 0}}}})
      (etaoin/screenshot-element driver
                                 {:tag :div :id :card}
                                 file-path)
      (finally
        (when (.exists html-path)
          (.delete html-path))))))

(defn- create-screenshots [powerpack-app]
  (let [resource-path (get-in powerpack-app [:imagine/config :resource-path])
        resource-dir (first (:powerpack/resource-dirs powerpack-app))
        db (d/db (:datomic/conn powerpack-app))
        cache-file (opengraph-cache-file)
        cache (read-opengraph-cache cache-file)
        cards (->> (d/q '[:find [?e ...]
                          :where
                          [?e :blog-post/author]]
                        db)
                   (map #(opengraph-card powerpack-app resource-dir resource-path (d/entity db %))))
        cards-to-regenerate (filter #(regenerate-opengraph? cache %) cards)
        driver (etaoin/chrome {:headless true})]

    (when-let [url (:site/base-url powerpack-app)]
      (let [{:keys [width height]} (etaoin/get-window-size driver)]
        (try
          (etaoin/go driver url)
          (etaoin/set-window-size driver 720 540)
          (etaoin/screenshot driver (io/file resource-dir resource-path "screenshots" "main_wide.png"))
          (etaoin/set-window-size driver 540 720)
          (etaoin/screenshot driver (io/file resource-dir resource-path "screenshots" "main_mobile.png"))
          (finally
            (etaoin/set-window-size driver width height)))))

    (doseq [card cards-to-regenerate]
      (screenshot-opengraph! driver card))

    (let [updated-cache (into {} (map (juxt :post-name cache-entry) cards))]
      (when (not= cache updated-cache)
        (write-opengraph-cache! cache-file updated-cache)))))

(defn- chrome-installed?
  "Checks if Google Chrome is installed."
  []
  (let [os-name (str/lower-case (System/getProperty "os.name"))
        traditional (if (str/includes? os-name "mac")
                      (if (.exists (io/file "/Applications/Google Chrome.app")) 0 1)
                      (:exit (sh/sh "which" "google-chrome")))
        chromedriver (:exit (sh/sh "which" "chromedriver"))]
    (= traditional chromedriver 0)))

(defn on-ingested [powerpack-app _results]
  (when (chrome-installed?)
    (create-screenshots powerpack-app))
  (->> (for [tag (d/q '[:find [?tag ...]
                        :where
                        [_ :blog-post/tags ?tag]]
                      (d/db (:datomic/conn powerpack-app)))]
         {:page/uri (str "/tag/" (name tag) "/")
          :page/kind :page.kind/tag
          :tag-page/tag tag})

       (d/transact (:datomic/conn powerpack-app))
       deref))
