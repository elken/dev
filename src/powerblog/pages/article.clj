(ns powerblog.pages.article
  (:require
   [phosphor.icons :as icons]
   [powerblog.components :as components]
   [powerpack.hiccup :as ph]
   [powerblog.layout :as layout]
   [powerblog.util :as util]
   [powerpack.markdown :as md]))

(defn count-words [body]
  (-> body
      (.split "\\W+")
      seq
      count))

(defn reading-time [body]
  (-> body
      count-words
      (/ 250)
      Math/ceil
      int))

(defn- page-header [page]
  (let [page-body (:page/body page)
        author (:blog-post/author page)
        created-date (:blog-post/created-at page)]
    [:div.flex.flex-col.justify-between.items-center.not-prose.md:flex-row
     (components/author author)
     [:div.flex.items-center
      (util/format-date created-date)
      (when-let [edited-date (:blog-post/edited-at page)]
        [:span.cursor-pointer.ml-2.border-b.border-dotted
         {:title
          (str "Last edited on "
               (util/format-date edited-date))}
         (icons/render :phosphor.regular/pen {:size "20px"
                                              :style {:display "block"}})])]
     [:span.cursor-pointer.decoration-dotted.underline
      {:title (str (count-words page-body) " words read at an average of 250 words per minute!")}
      (reading-time page-body) " min read"]]))

(defn render-page [context page]
  (let [{:page/keys [body title]
         :blog-post/keys [preview]
         :open-graph/keys [description]} page]
    (layout/layout
     (merge context {:title title})
     page
     [:article.prose.dark:prose-invert.mx-auto.prose-slate.lg:prose-lg.prose-a:no-underline
      [:h1.text-nord-9.dark:text-nord-8 (md/render-html title)]
      (when description
        [:h4 (md/render-html description)])
      (page-header page)
      (md/render-html preview)
      (md/render-html body)
      [:div.flex.gap-3
       (for [tag (:blog-post/tags page)]
         (components/tag {:body (name tag)
                          :name (name tag)}))]]
     [:script {:id "comments"
               :src "https://giscus.app/client.js"
               :data-repo "elken/dev"
               :data-repo-id "R_kgDOK4IZYw"
               :data-category "Ideas"
               :data-category-id "DIC_kwDOK4IZY84CbpI1"
               :data-mapping "pathname"
               :data-strict "0"
               :data-reactions-enabled "1"
               :data-emit-metadata "0"
               :data-input-position "top"
               :data-theme "preferred_color_scheme"
               :data-lang "en"
               :crossorigin "anonymous"
               :async true}])))
