(ns powerblog.pages.blog-listing
  (:require [powerblog.layout :as layout]
            [powerblog.pages.frontpage :as frontpage]))

(defn render-page [context page]
  (layout/layout
   {:title [:i18n ::page-title]}
   page
   [:h1 [:i18n ::page-title]]
   (for [blog-post (frontpage/get-blog-posts (:app/db context))]
     [:article.prose.dark:prose-invert.mx-auto.shadow.rounded.prose-nord-3.prose-a:no-underline.prose-headings:my-1.prose-p:mt-1.dark:bg-nord-2
      [:div "hi"]
      [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]]])))
