(ns powerblog.pages.webmanifest
  (:require
   [clojure.data.json :as json]
   [optimus.link :as link]))

(defn render-manifest [context]
  (let [{:powerpack/keys [app]} context
        {:site/keys [title short-name description base-url]} app]
    (json/write-str
     {:name title
      :short_name short-name
      :description description
      :start_url base-url
      :display "standalone"
      :orientation "portrait-primary"
      :scope "/"
      :icons [{:src (link/file-path context "/android-chrome-192x192.png")
               :sizes "192x192"
               :type "image/png"
               :purpose "any"}
              {:src (link/file-path context "/android-chrome-512x512.png")
               :sizes "512x512"
               :type "image/png"
               :purpose "any"}]
      :screenshots [{:src (link/file-path context "/screenshots/main_wide.png")
                     :type "image/png"
                     :sizes "720x540"
                     :form_factor "wide"}
                    {:src (link/file-path context "/screenshots/main_mobile.png")
                     :type "image/png"
                     :sizes "540x720"
                     :form_factor "narrow"}]})))

(defn render-oembed [context]
(let [{:powerpack/keys [app]} context
        {:site/keys [title author base-url]} app]
    (json/write-str
     {:author_name author
      :provider_name title
      :provider_url base-url})))
