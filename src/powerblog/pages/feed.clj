(ns powerblog.pages.feed
  "Courtesy of https://github.com/borkdude/quickblog"
  (:require
   [clojure.data.xml :as xml]
   [powerblog.pages.blog-listing :as blog-listing]
   [powerpack.markdown :as md])
  (:import
   java.time.format.DateTimeFormatter))

(xml/alias-uri 'atom "http://www.w3.org/2005/Atom")

(defn- rfc-3339-now []
  (let [fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ssxxx")
        now (java.time.ZonedDateTime/now java.time.ZoneOffset/UTC)]
    (.format now fmt)))

(defn- rfc-3339 [yyyy-MM-dd]
  (let [in-fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd")
        local-date (java.time.LocalDate/parse yyyy-MM-dd in-fmt)
        fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ssxxx")
        now (java.time.ZonedDateTime/of (.atTime local-date 23 59 59) java.time.ZoneOffset/UTC)]
    (.format now fmt)))

(defn- atom-feed
  ;; validate at https://validator.w3.org/feed/check.cgi
  [config posts]
  (let [{:site/keys [title base-url author]} config]
    (-> (xml/sexp-as-element
         [::atom/feed
          {:xmlns "http://www.w3.org/2005/Atom"}
          [::atom/title title]
          [::atom/link {:href (str base-url "/atom.xml") :rel "self"}]
          [::atom/link {:href base-url}]
          [::atom/updated (rfc-3339-now)]
          [::atom/id base-url]
          [::atom/author
           [::atom/name author]]
          (for [{:page/keys [body uri title]
                 :blog-post/keys [preview created-at edited-at]} posts
                :let [link (str base-url uri)]]
            [::atom/entry
             [::atom/id link]
             [::atom/link {:href link}]
             [::atom/title title]
             [::atom/updated (rfc-3339 (or edited-at created-at))]
             [::atom/content {:type "html"}
              [:-cdata (md/render-html (str preview body))]]])])
        xml/indent-str)))

(defn- clojure-post? [{:blog-post/keys [tags]}]
  (some #{:clojure :clojurescript} tags))

(defn render-atom-feed [context]
  (atom-feed (:powerpack/app context) (blog-listing/get-blog-posts (:app/db context))))

(defn render-planetclojure-feed [context]
  (atom-feed
   (:powerpack/app context)
   (->> context
        :app/db
        blog-listing/get-blog-posts
        (filter clojure-post?))))

;; (defn- spit-feeds [{:keys [out-dir modified-posts posts] :as opts}]
;;   (let [feed-file (fs/file out-dir "atom.xml")
;;         clojure-feed-file (fs/file out-dir "planetclojure.xml")
;;         all-posts (lib/sort-posts (vals posts))
;;         clojure-posts (->> (vals posts)
;;                            (filter clojure-post?)
;;                            lib/sort-posts)
;;         clojure-posts-modified? (->> modified-posts
;;                                      (map posts)
;;                                      (some clojure-post?))]
;;     (if (and (not clojure-posts-modified?) (fs/exists? clojure-feed-file))
;;       (println "No Clojure posts modified; skipping Clojure feed")
;;       (do
;;         (println "Writing Clojure feed" (str clojure-feed-file))
;;         (spit clojure-feed-file (atom-feed opts clojure-posts))))
;;     (if (and (empty? modified-posts) (fs/exists? feed-file))
;;       (println "No posts modified; skipping main feed")
;;       (do
;;         (println "Writing feed" (str feed-file))
;;         (spit feed-file (atom-feed opts all-posts))))))
