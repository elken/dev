(ns powerblog.pages.frontpage
  (:require
   [datomic.api :as d]
   [phosphor.icons :as icons]
   [powerblog.components :as components]
   [powerblog.layout :as layout]
   [powerblog.util :as util]
   [powerpack.markdown :as md]))

(defn get-blog-posts [db]
  (->> (d/q '[:find [?e ...]
              :where
              [?e :blog-post/author]]
            db)
       (map #(d/entity db %))))

(defn render-page [context page]
  (let [blog-posts (get-blog-posts (:app/db context))]
    (layout/layout
     (merge context {:title "Posts"})
     page
     [:div.prose.dark:prose-invert.prose-nord-3.prose-a:no-underline.prose-headings:my-1.prose-p:mt-1.mx-auto
      (md/render-html (:page/body page))
      (for [{:page/keys [uri title]
             :blog-post/keys [created-at tags preview]
             :open-graph/keys [image description]} blog-posts]
        [:article.shadow.rounded.dark:bg-nord-2.my-3
         (when image
           [:img.rounded-t.h-32.w-full.mb-0.object-cover.object-center {:src image}])
         [:div.px-4.pb-1
          [:h2
           [:a {:id ""
                :href uri} title]]
          (when description
            [:h4.text-nord-1.dark:text-nord-4.text-lg.font-semibold
             (md/render-html description)])
          [:span.flex.flex-col.sm:flex-row.gap-3.items-center.mb-3
           [:span.select-none
            (icons/render :phosphor.fill/calendar {:size 20
                                                   :class "mr-2"})
            (util/format-date created-at)]
           [:div.flex.gap-3
            (for [tag tags]
              (components/tag {:body (name tag)
                               :name (name tag)}))]]
          (md/render-html preview)]])])))
