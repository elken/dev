(ns powerblog.pages.frontpage
  (:require
   [powerblog.layout :as layout]))

(defn article-item [{:keys [title summary post slides recording]}]
  [:li {:class "flex flex-col items-start p-4 bg-nord-5 hover:bg-nord-5/80 dark:bg-nord-2 dark:hover:bg-nord-2/80 hover:cursor-pointer"}
   [:div.flex.flex-col.items-start
    [:h3.font-bold.text-lg title]
    [:h5 summary]]
   [:div.flex.gap-x-4
    (when post
      [:a {:href post
           :target "_blank"
           :rel "noopener noreferrer"} "Article"])
    (when slides
      [:a {:href slides
           :target "_blank"
           :rel "noopener noreferrer"}
       "Slides"])
    (when recording
      [:a {:href recording
           :target "_blank"
           :rel "noopener noreferrer"}
       "Recording"])]])

(defn language-slider [languages]
  [:div.md:flex.md:gap-4.grid.grid-rows-2.grid-cols-6.gap-x-8
   (for [language languages]
     [:i
      {:class (format "text-2xl devicon-%1$s-plain devicon-%1$s-original colored" language)
       :title language}])])

(defn render-page [context page]
  (layout/layout
   (merge context {:title "Portfolio" :padding? false})
   page
   [:ul#scroll-nav.fixed.top-4.md:flex.flex-row.z-50.gap-4.hidden
    {:class "right-1/2"}
    [:li
     [:a {:href "#portfolio"} "Portfolio"]]
    [:li
     [:a {:href "#publications"} "Publications"]]
    [:li
     [:a {:href "#contact"} "Contact"]]]

   [:ul#mobile-scroll-nav.fixed.top-14.left-0.md:hidden.flex.flex-row.z-50.w-full.bg-nord-5.dark:bg-nord-0
    [:li.flex-1.text-center
     [:a.hover:bg-transparent.hover:!text-nord-2.hover:dark:!text-nord-4 {:href "#portfolio"} "Portfolio"]]
    [:li.flex-1.text-center
     [:a.hover:bg-transparent.hover:!text-nord-2.hover:dark:!text-nord-4 {:href "#publications"} "Publications"]]
    [:li.flex-1.text-center
     [:a.hover:bg-transparent.hover:!text-nord-2.hover:dark:!text-nord-4 {:href "#contact"} "Contact"]]]

   [:section#top.min-h-screen.flex.items-center.justify-center
    [:div {:class "flex flex-col md:flex-row md:items-center md:justify-between w-2/3"}
     [:div
      [:h1 {:class "text-2xl md:text-5xl font-bold mt-6 mb-4 leading-tight"}
       "Clean, Modern Websites"
       [:br]
       "from Design to Deployment"]
      [:p {:class "mb-2 md:mb-8"}
       "Explore " [:a {:href "#portfolio"} "my work"] "  or get in touch to start your project today."]
      [:div
       (language-slider ["javascript" "java" "json" "csharp" "graphql" "react" "typescript" "clojure" "clojurescript" "elixir" "blazor" "emacs" "bash" "python" "flutter"])]]
     [:div {:class "md:w-96 flex justify-center mt-8 md:mt-0"}
      [:div {:class "p-2 bg-nord-4 dark:bg-nord-3 rounded-2xl shadow-xl transform rotate-6"}
       [:div {:class "p-2 bg-nord-9 rounded-2xl transform -rotate-6"}
        [:img {:src "/images/lkn.png"
               :alt "Your Portrait"
               :class "rounded-3xl shadow-lg"}]]]]]]
   
   [:section#portfolio {:class "min-h-screen flex items-center justify-center bg-nord-4 dark:bg-nord-3"}
    [:div {:class "text-center mt-4"}
     [:h2 {:class "text-4xl font-bold mb-6"} "Previous work"]
     [:p.mb-4
      "Below are some examples of previous applications I've written. Click the image to view it larger and read more about it. Most of my
      career has been spent working on internal applications so there's not a
      lot I can point to publicly; but I'd love to change that!"]
     [:p.mb-4
      "You can also check my " [:span [:a {:href "https://github.com/elken/"
                                           :target "_blank"
                                           :rel "noopener noreferrer"}
                                       "GitHub"]] " for more of my open-source contributions."]
     [:div.inline-grid.gap-4
      {:class "grid-cols-[auto_auto_auto]"}
      [:img.spotlight.cursor-pointer.h-48 {:src "/images/portfolio/dashboard-1.png"
                                           :alt "My first production app; called 'The Dashboard'. ReactJS & C#"
                                           :data-description "This application encompassed all the useful data for the company, and was designed to be displayed on TVs across the business; hence the ugly 'buckets'. Over time it grew more and more complex as new pages were requested, but it was very stable and has been running without development since I worked on it."}]
      [:img.spotlight.cursor-pointer.h-48 {:src "/images/portfolio/veritas-1.png"
                                           :alt "Veritas; an internal issue tracking system. Blazor & C#"
                                           :data-description "Due to the nature of the business, this was also built to be a live application, updating as new issues are raised and new titles enter the system. The Submit Issue button on the bottom right was used to garner user feedback as this was an alpha product."}]
      [:img.spotlight.cursor-pointer.h-48 {:src "/images/portfolio/veritas-2.png"
                                           :alt "Veritas Issue Page. Blazor & C#"
                                           :data-description "Again being an alpha product; the UX leaves a bit to be desired. Users are able to comment on the issues RE: status changes; and on the bottom left there's a collapsible audit log of every change the issue underwent, including who made it and when."}]
      [:img.spotlight.cursor-pointer.h-48 {:src "/images/portfolio/veritas-3.png"
                                           :alt "Veritas Edit Issue. Blazor & C#"
                                           :data-description "Because these issues are intended to be updated live by anyone, a locking mechanism had to be implemented whenever someone tries to edit. In this state, nobody else is able to edit anything; however users with Admin access can forcibly unlock the issue if it were to get stuck. It also features a 'Job Lookup' component on the left which pulled data from various internal systems to make data input as simple as possible."}]
      [:img.spotlight.cursor-pointer.h-48 {:src "/images/portfolio/bk.png"
                                           :alt "Bank of Kigali app. Flutter"
                                           :data-description "I spent many months working as a core contributor to this app."}]
      [:img.spotlight.cursor-pointer.h-48 {:src "/images/portfolio/excel_dashboard.png"
                                           :alt "Sand Tracking Dashboard. ClojureScript & Clojure"
                                           :data-description "Another live-updating dashboard, this time with a much nicer UX."}]]]]

   [:section#publications.min-h-screen.flex.items-center.justify-center
    [:div.text-center
     [:h2.text-4xl.font-bold.mb-6 "Publications"]
     [:p.mt-4
      "Outside of development, I've also published the following articles & talks"
      [:ul {:class "divide-y divide-gray-200 dark:divide-gray-600"}
       (article-item
        {:title "Git (Or how I learned to stop worrying and love version control)"
         :summary "An introductory workshop on using Git"
         :slides "https://github.com/elken/talks/blob/master/git-introduction/git.pdf"})
       (article-item
        {:title "Org-a-nice Your Life in Emacs"
         :summary "How to effectively use Emacs' org-mode"
         :slides "https://github.com/elken/talks/blob/master/org-a-nice-your-life-in-emacs/slides.pdf"})
       (article-item
        {:title "State of Doom Emacs"
         :summary "An overview of Doom Emacs & where it's heading"
         :post "https://www.juxt.pro/blog/safari-doom-emacs/"
         :slides "https://github.com/elken/talks/blob/master/state-of-doom-2023/state-of-doom.pdf"
         :recording "https://www.youtube.com/watch?v=PwSscfkfRgw"})
       (article-item
        {:title "Using Clerk for Advent of Code"
         :summary "Using Clojure notebooks to solve programming problems"
         :post "https://www.juxt.pro/blog/using-clerk-for-aoc/"})
       (article-item
        {:title "Advancedvent of Clerkjure"
         :summary "Building data visualisations in Clojure notebooks"
         :post "https://www.juxt.pro/blog/advanced-clerk-usage/"})]]]]

   [:section#contact {:class "h-[94dvh] flex items-center justify-center bg-nord-4 dark:bg-nord-3"}
    [:div {:class "text-center"}
     [:h2 {:class "text-4xl text-nord4 font-bold mb-6"} "Get in touch!"]
     [:p {:class "text-nord3 mb-4"}
      "I'm available for work currently, so do get in touch if you want to chat and get a copy of my CV!"]

     [:div.grid.grid-cols-1.gap-x-8.gap-y-8.md:grid.cols-3
      [:form {:action "https://formspree.io/f/mpzvjezr" :method "POST" :class "p-6 flex flex-col justify-center"}
       [:div {:class "flex flex-col mt-2"}
        [:label {:for "name" :class "hidden"} "name"]
        [:input {:name "name"
                 :id "name"
                 :required true
                 :placeholder "Full Name"
                 :class "w-full mt-2 py-3 px-3 rounded-lg bg-nord-5 dark:bg-nord-3 focus:outline-none focus:border-nord-15 focus:ring-nord-15"}]]
       [:div {:class "flex flex-col mt-2"}
        [:label {:for "email" :class "hidden"} "email"]
        [:input {:type "email"
                 :name "email"
                 :id "email"
                 :required true
                 :placeholder "Email Address"
                 :class "w-full mt-2 py-3 px-3 rounded-lg bg-nord-5 dark:bg-nord-3 focus:outline-none focus:border-nord-15 focus:ring-nord-15"}]]
       [:div {:class "flex flex-col mt-2"}
        [:label {:for "message" :class "hidden"} "message"]
        [:textarea {:rows "3"
                    :name "message"
                    :id "message"
                    :required true
                    :placeholder "Enter your message"
                    :class "w-full mt-2 py-3 px-3 rounded-lg bg-nord-5 dark:bg-nord-3 focus:outline-none focus:border-nord-15 focus:ring-nord-15"}]
        [:button {:type "submit"
                  :onClick "submitContactForm()"
                  :class "md:w-32 bg-nord-9 hover:bg-nord-9/70 text-white font-bold py-3 px-6 rounded-lg mt-3 transition ease-in-out duration-300"}
         "Submit"]]]]]]
   [:script {:src "https://rawcdn.githack.com/lcdsantos/menuspy/db2c761b6afca35af2e4696084a2d75437e4725b/dist/menuspy.min.js"}]
   [:script "
 new MenuSpy(document.querySelector('#scroll-nav'), {activeClass: 'active-link'})
 new MenuSpy(document.querySelector('#mobile-scroll-nav'), {activeClass: 'active-box'})
"]))
