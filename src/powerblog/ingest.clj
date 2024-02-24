(ns powerblog.ingest
  (:require
   [clojure.java.io :as io]
   [clojure.java.shell :as sh]
   [clojure.string :as str]
   [datomic.api :as d]
   [etaoin.api :as etaoin]
   [powerblog.pages.opengraph :as opengraph]))

(defn get-page-kind [file-name]
  (cond
    (re-find #"^blog-posts/" file-name)
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

(defn- create-screenshots [powerpack-app]
  (let [resource-path (get-in powerpack-app [:imagine/config :resource-path])
        resource-dir (first (:powerpack/resource-dirs powerpack-app))
        db (d/db (:datomic/conn powerpack-app))
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

    (doall
     (for [post (d/q '[:find [?e]
                       :where
                       [?e :blog-post/author]]
                     db)]
       (let [post (d/entity db post)
             post-name (last (str/split (:page/uri post) #"/"))
             file-path (io/file resource-dir resource-path "opengraph" (str post-name ".png"))
             html-path (java.io.File/createTempFile post-name ".html")]
         (try
           (spit html-path (opengraph/render-opengraph-page powerpack-app post))
           (etaoin/go driver (str "file://" html-path))
           ;; send the Chrome-specific request for a transparent background
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
               (.delete html-path)))))))))

(defn- chrome-installed?
  "Checks if Google Chrome is installed."
  []
  (let [traditional (:exit (sh/sh "which" "google-chrome"))
        chromedriver (:exit (sh/sh "which" "chromedriver"))]
    (= traditional chromedriver 0)))

(defn on-ingested [powerpack-app results]
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
