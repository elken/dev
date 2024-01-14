(ns powerblog.pages.tag
  (:require [datomic.api :as d]
            [powerblog.layout :as layout]
            [powerblog.components :as components]
            [powerblog.pages.frontpage :as frontpage]))

(defn get-blog-posts [db tag]
  (->> (d/q '[:find [?e ...]
              :in $ ?tag
              :where
              [?e :blog-post/tags ?tag]]
            db tag)
       (map #(d/entity db %))))

(defn render-tag-page [context page]
  (let [title (str "Blog posts about " (name (:tag-page/tag page)))]
    (layout/layout
     (merge context {:title title})
     page
     [:div.prose.dark:prose-invert.prose-nord-3.prose-a:no-underline.prose-headings:my-1.prose-p:mt-1.mx-auto
      [:h1.text-center title]
      [:ul
       (for [blog-post (get-blog-posts (:app/db context) (:tag-page/tag page))]
         (frontpage/article-page blog-post))]])))

(defn render-tags-page [context page]
  (layout/layout
   (merge context {:title "Tags"})
   page
   [:div.max-w-2xl.mx-auto.p-4.bg-nord-6.text-nord-0.dark:bg-nord-1.dark:text-nord-5
    [:h2.text-5xl.font-bold.mb-4.text-nord-9.dark:text-nord-8 "Tags"]
    [:ul
     (for [[tag count] (->> (d/q '[:find ?e ?tags
                                   :where
                                   [?e :blog-post/tags ?tags]]
                                 (:app/db context))
                            (map second)
                            frequencies)]
       (components/tag {:body (str count "x " (name tag))
                        :name (name tag)}))]]))
