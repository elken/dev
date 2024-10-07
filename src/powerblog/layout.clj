(ns powerblog.layout
  (:require
   [hiccup.page :refer [include-css include-js]]
   [optimus.link :as link]))

(defn colour-mode-toggle []
  [:button#theme-toggle.py-1
   [:svg#theme-toggle-dark.w-4.h-4.hover:text-nord-0.hidden.fill-current
    {:viewBox "0 0 20 20"
     :xmlns "http://www.w3.org/2000/svg"}
    [:path {:d "M17.293 13.293A8 8 0 016.707 2.707a8.001 8.001 0 1010.586 10.586z"}]]
   [:svg#theme-toggle-light.w-4.h-4.hover:text-nord-13.hidden.fill-current
    {:viewBox "0 0 20 20"
     :xmlns "http://www.w3.org/2000/svg"}
    [:path {:d "M10 2a1 1 0 011 1v1a1 1 0 11-2 0V3a1 1 0 011-1zm4 8a4 4 0 11-8 0 4 4 0 018 0zm-.464 4.95l.707.707a1 1 0 001.414-1.414l-.707-.707a1 1 0 00-1.414 1.414zm2.12-10.607a1 1 0 010 1.414l-.706.707a1 1 0 11-1.414-1.414l.707-.707a1 1 0 011.414 0zM17 11a1 1 0 100-2h-1a1 1 0 100 2h1zm-7 4a1 1 0 011 1v1a1 1 0 11-2 0v-1a1 1 0 011-1zM5.05 6.464A1 1 0 106.465 5.05l-.708-.707a1 1 0 00-1.414 1.414l.707.707zm1.414 8.486l-.707.707a1 1 0 01-1.414-1.414l.707-.707a1 1 0 011.414 1.414zM4 11a1 1 0 100-2H3a1 1 0 000 2h1z"
            :fill-rule "evenodd"
            :clip-rule "evenodd"}]]])

(defn nav-menu []
  [:ul#nav-menu.sm:flex.w-full.sm:w-auto.hidden.mt-2.sm:mt-0.sm:space-x-2
   [:li
    [:a {:href "/posts/"} "Posts"]]
   [:li
    [:a {:href "/tags/"} "Tags"]]
   [:li
    [:a {:href "https://github.com/elken/dev/discussions/categories/comments"
         :target "_blank"
         :rel "noopener noreferrer"}
     "Discuss"]]
   [:li
    [:a {:href "/atom.xml"} "Feed"]]
   [:li
    [:a {:href "/about/"} "About"]]
   [:li
    (colour-mode-toggle)]])

(defn header []
  [:header.bg-nord-5.dark:bg-nord-0.mb-2.w-full.fixed.px-6.sm:px-7.xl:px-8.2xl:px-10.py-2.z-50
   [:nav.flex.items-center.justify-between.flex-wrap
    [:a {:href "/"}
     [:img.h-10 {:src "/logo/images/lkn.png"}]]
    [:input#nav-toggle.hidden {:type "checkbox"}]
    [:label#show-button.sm:hidden.text-slate-600.dark:text-slate-400.hover:text-sky-500
     {:for "nav-toggle"}
     [:svg.fill-current.h-4.w-4 {:viewBox "0 0 20 20"
                                 :xmlns "http://www.w3.org/2000/svg"}
      [:title "Menu Open"]
      [:path {:d "M0 3h20v2H0V3z m0 6h20v2H0V9z m0 6h20v2H0V0z"}]]]
    [:label#hide-button.hidden.text-slate-600.dark:text-slate-400.hover:text-sky-500
     {:for "nav-toggle"}
     [:svg.fill-current.h-4.w-4 {:viewBox "0 0 20 20"
                                 :xmlns "http://www.w3.org/2000/svg"}
      [:title "Menu Close"]
      [:polygon {:points "11 9 22 9 22 11 11 11 11 22 9 22 9 11 -2 11 -2 9 9 9 9 -2 11 -2"
                 :transform "rotate(45 10 10)"}]]]
    (nav-menu)]])

(defn footer []
  [:footer.bg-nord-5.dark:bg-nord-0.w-full.p-4.text-xs.text-center
   [:p
    "Made with "
    [:a.underline.hover:text-sky-400 {:href "https://github.com/cjohansen/powerpack"} "Powerpack"]
    " and "
    [:a.underline.hover:text-sky-400 {:href "https://tailwindcss.com"} "TailwindCSS"]
    ". Source code available "
    [:a.underline.hover:text-sky-400 {:href "https://github.com/elken/dev"} "here!"]]])

(defn layout [{:keys [title padding?] :as context
               :or {padding? true}}
              {:blog-post/keys [header-image]} & content]
  (let [site-title (-> context :powerpack/app :site/title)]
    [:html
     [:head
      (when title [:title (str title (when site-title (str " | " site-title)))])
      [:link {:href "/atom.xml" :rel "alternate" :title "lkn's ramblings" :type "application/atom+xml"}]
      (when-let [href (link/file-path context "/safari-pinned-tab.svg")]
        [:link {:rel "mask-icon" :href href :color "#b48ead"}])
      [:meta {:name "msapplication-TileColor" :content "#b48ead"}]
      [:meta {:name "theme-color" :media "(prefers-color-scheme: light)" :content "#d8dee9"}]
      [:meta {:name "theme-color" :media "(prefers-color-scheme: dark)" :content "#2e3440"}]
      [:meta {:name "author" :content "Ellis Kenyő"}]
      [:meta {:name "twitter:card" :content "summary_large_image"}]
      [:link {:rel "manifest" :href "/manifest.json"}]
      [:link {:type "application/json+oembed" :href "/oembed.json"}]
      (include-css "https://cdn.jsdelivr.net/gh/devicons/devicon@latest/devicon.min.css")
      (include-css "https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css")
      (include-js "https://rawcdn.githack.com/nextapps-de/spotlight/0.7.8/dist/spotlight.bundle.js")
      [:script {:defer true
                :src "https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js"
                :integrity "sha384-XjKyOOlGwcjNTAIQHIpgOno0Hl1YQqzUOEleOLALmuqehneUG+vnGctmUb0ZY0l8"
                :crossorigin "anonymous"}]
      [:script {:defer true
                :src "https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js"
                :integrity "sha384-+VBxd3r6XgURycqtZ117nYw44OOcIax56Z4dCRWbxyPt0Koah1uHoK0o4+/RRE05"
                :crossorigin "anonymous"
                :onload "renderMathInElement(document.body);"}]
      [:script {:async true
                :src "https://www.googletagmanager.com/gtag/js?id=G-N4P230RNDE"}]
      [:script {:type "module"}
       "import mermaid from 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.esm.min.mjs';"]
      [:script
       "window.dataLayer = window.dataLayer || []; function gtag(){dataLayer.push(arguments);} gtag('js', new Date()); gtag('config', 'G-N4P230RNDE');"]]
     [:body.min-h-responsive-screen.flex.flex-col.bg-nord-5.dark:bg-nord-0.text-nord-3.dark:text-nord-4.line-numbers.rainbow-braces.match-braces.linkable-line-numbers.diff-highlight
      (header)
      [:main.mt-14.px-6.flex-1.overflow-hidden.prose-a:text-nord-9.prose-a:dark:text-nord-8
       [:div.h-full.rounded-lg.shadow-md.overflow-auto.bg-nord-6.dark:bg-nord-1
        (when header-image
          [:img.h-40.rounded-t.w-full.object-cover.object-center {:src header-image}])
        [:div.w-full.max-w-none
         {:class (when padding? " px-6 sm:px-7 xl:px-8 2xl:px-10 py-4")}
         content]]]
      (footer)]]))
