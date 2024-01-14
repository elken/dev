(ns powerblog.ingest
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [etaoin.api :as etaoin]
   [datomic.api :as d]
   [powerblog.pages.opengraph :as opengraph]))

(defonce driver (etaoin/chrome {:headless true
                                :path-driver (str (io/file (io/resource "chromedriver")))}))

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

(defn on-ingested [powerpack-app results]
  (let [resource-path (get-in powerpack-app [:imagine/config :resource-path])
        resource-dir (first (:powerpack/resource-dirs powerpack-app))
        db (d/db (:datomic/conn powerpack-app))]
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
               (.delete html-path)))))))
    (->> (for [tag (d/q '[:find [?tag ...]
                          :where
                          [_ :blog-post/tags ?tag]]
                        db)]
           {:page/uri (str "/tag/" (name tag) "/")
            :page/kind :page.kind/tag
            :tag-page/tag tag})

         (d/transact (:datomic/conn powerpack-app))
         deref)))
