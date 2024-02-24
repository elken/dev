(ns powerblog.components)

(defn tag [{:keys [body name]}]
  [:a.inline-block.bg-nord-9.text-nord-6.text-xs.font-medium.mr-2.rounded.no-underline.not-prose.hover:!text-nord-6
   {:class "px-2.5 py-0.5 hover:opacity-90"
    :href (str "/tag/" name "/")}
   [:svg.inline.fill-nord-10
    {:xmlns "http://www.w3.org/2000/svg"
     :height "1em"
     :viewBox "0  0 448 512"}
    [:path
     {:d "M0 80V229.5c0 17 6.7 33.3 18.7 45.3l176 176c25 25 65.5 25 90.5 0L418.7 317.3c25-25 25-65.5 0-90.5l-176-176c-12-12-28.3-18.7-45.3-18.7H48C21.5 32 0 53.5 0 80zm112 32a32 32 0 1 1 0 64 32 32 0 1 1 0-64z"}]]
   [:span.pl-1 body]])

(defn author [{:person/keys [id full-name]}]
  [:div.flex.items-center.gap-3
   [:img.m-0.h-14.no-spotlight {:src (str "/logo/images/" (name id) ".png")}]
   full-name])
