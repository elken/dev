(ns powerblog.pages.opengraph
  (:require
   [hiccup.core :as hiccup]
   [powerblog.components :as components]
   [powerblog.pages.article :refer [reading-time]]
   [powerpack.assets :as assets]
   [powerpack.markdown :as md]
   [powerpack.hiccup :as ph])
  (:import java.util.Base64))

(defn image-to-base64 [image]
  (let [byte-array-os (java.io.ByteArrayOutputStream.)]
    (javax.imageio.ImageIO/write image "png" byte-array-os)
    (let [byte-array (.toByteArray byte-array-os)]
      (str "data:image/png;base64,"
           (.encodeToString (Base64/getEncoder) byte-array)))))

(defn opengraph-image [context blog-post]
  (let [{:person/keys [id full-name]} (:blog-post/author blog-post)
        assets (assets/get-assets context)
        header (ph/load-image (:imagine/config context) (:blog-post/header-image blog-post))
        logo (ph/load-image (:imagine/config context) (str "/logo/images/" (name id) ".png"))
        main-css (:contents (first (filter #(= (:path %) "/styles.css") assets)))]
    [:html
     [:style
      main-css]
     [:body.flex.items-center.justify-center.h-screen
      [:div#card.rounded-lg.overflow-hidden.w-full.max-w-xl.shadow-lg
       [:div.relative
        [:img.w-full.object-cover.h-80 {:src (image-to-base64 header)}]
        [:div.absolute.inset-0.flex.flex-col.justify-center.text-nord-6
         {:class "bg-nord-1/90"}
         [:div.absolute.top-0.left-0.p-4
          [:div.flex.items-center.gap-3
           [:img.m-0.h-14 {:src (image-to-base64 logo)}]
           full-name]]
         [:div.text-center.mx-auto.px-5
          [:h2.text-2xl.font-bold.text-shadow.mb-2
           (md/render-html
            (:page/title blog-post))]
          [:p.text-lg.text-shadow.mb-4
           (md/render-html (:blog-post/preview blog-post))]]
         [:div.absolute.bottom-0.left-0.right-0.p-4.flex.justify-end.space-x-2
          (for [tag (:blog-post/tags blog-post)]
            (components/tag {:body (name tag)
                             :name (name tag)}))]
         [:div.absolute.bottom-0.left-0.right-0.p-4.flex.justify-between.items-end
          [:span.bg-nord-9.text-xs.font-medium.px-2.py-1.rounded
           (reading-time (:page/body blog-post)) " minute read"]]]]]]]))

(defn render-opengraph-page [context blog-post]
  (hiccup/html (opengraph-image context blog-post)))
