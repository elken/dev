(ns powerblog.pages.about
  (:require
   [powerpack.markdown :as md]
   [powerblog.layout :as layout]
   [phosphor.icons :as icons]))

(defn render-page [context {:about/keys [github gitlab linkedin] :as page}]
  (layout/layout
   (merge context {:title "About"})
   page
   [:div.mx-auto.prose.prose-slate.lg:prose-lg.prose-a:no-underline.dark:prose-invert
    (md/render-html (:page/body page))
    [:div.pt-3.flex.flex-row.items-center.justify-around
     (when github
       [:a.transition.ease-in-out.duration-200.hover:-translate-y-1.hover:scale-110.rounded-full.p-2.!text-white
        {:class "hover:!text-[#171515] bg-[#171515]"
         :target "_blank"
         :ref "noopener noreferrer"
         :href (str "https://github.com/" github)}
        (icons/render :phosphor.regular/github-logo {:size 64})])
     (when gitlab
       [:a.transition.ease-in-out.duration-200.hover:-translate-y-1.hover:scale-110.rounded-full.p-2.!text-white
        {:class "hover:!text-[#e5543c] bg-[#e5543c]"
         :target "_blank"
         :ref "noopener noreferrer"
         :href (str "https://gitlab.com/" gitlab)}
        (icons/render :phosphor.regular/gitlab-logo {:size 64})])
     (when linkedin
       [:a.transition.ease-in-out.duration-200.hover:-translate-y-1.hover:scale-110.rounded-full.p-2.!text-white
        {:class "hover:!text-[#0072b1] bg-[#0072b1]"
         :target "_blank"
         :ref "noopener noreferrer"
         :href (str "https://linkedin.com/in/" linkedin)}
        (icons/render :phosphor.regular/linkedin-logo {:size 64})])]]))
