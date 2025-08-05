(ns powerblog.core
  (:require
   [powerblog.ingest :as ingest]
   [powerblog.pages :as pages]
   [powerpack.markdown :as md])
  (:import
   [com.vladsch.flexmark.ext.anchorlink AnchorLinkExtension]
   [com.vladsch.flexmark.ext.attributes AttributesExtension]
   [com.vladsch.flexmark.ext.autolink AutolinkExtension]
   [com.vladsch.flexmark.ext.footnotes FootnoteExtension]
   [com.vladsch.flexmark.ext.gfm.strikethrough StrikethroughSubscriptExtension]
   [com.vladsch.flexmark.ext.gitlab GitLabExtension]
   [com.vladsch.flexmark.ext.tables TablesExtension]
   [com.vladsch.flexmark.ext.typographic TypographicExtension]
   [com.vladsch.flexmark.parser Parser]
   [com.vladsch.flexmark.util.data MutableDataSet]))

(def flexmark-opts
  (-> (MutableDataSet.)
      (.set AttributesExtension/ASSIGN_TEXT_ATTRIBUTES true)
      (.set AttributesExtension/FENCED_CODE_INFO_ATTRIBUTES true)
      (.set Parser/EXTENSIONS [(AutolinkExtension/create)
                               (AnchorLinkExtension/create)
                               (AttributesExtension/create)
                               (FootnoteExtension/create)
                               (GitLabExtension/create)
                               (StrikethroughSubscriptExtension/create)
                               (TablesExtension/create)
                               (TypographicExtension/create)])))

(alter-var-root #'md/flexmark-opts (constantly flexmark-opts))

(def config
  {:site/title "lkn's ramblings"
   :site/short-name "elken.dev"
   :site/description "A selection of ramblings about tech, clojure & whatever else is on my mind"
   :site/author "Ellis Kenyő"
   :site/base-url "https://elken.dev"
   :powerpack/render-page #'pages/render-page
   :powerpack/create-ingest-tx #'ingest/create-tx
   :powerpack/on-ingested #'ingest/on-ingested
   :powerpack/log-level :debug

   :optimus/bundles {"app.css"
                     {:public-dir "public"
                      :paths ["/styles.css"]}

                     "prism.css"
                     {:public-dir "public"
                      :paths ["/prism.css"
                              "/prism-nord.css"]}

                     "prism.js"
                     {:public-dir "public"
                      :paths ["/prism.min.js"]}

                     "app.js"
                     {:public-dir "public"
                      :paths ["/js/theme-toggle.js"
                              "/js/lightbox-setup.js"
                              "/js/clickable-headings.js"]}}

   :optimus/assets [{:public-dir "public"
                     :paths ["/favicon.ico"
                             #".*\.jpg"
                             #".*\.svg"
                             #".*\.png"]}]

   :imagine/config {:prefix "image-assets"
                    :resource-path "public"
                    :disk-cache? true
                    :transformations
                    {:header
                     {:transformations [[:resize {:width 1920 :scale-up? true}]]}
                     :logo
                     {:transformations [[:fit {:width 64 :height 64}]]
                      :width 64}
                     :preview-small
                     {:transformations [[:fit {:width 184 :height 184}]
                                        [:crop {:preset :square}]]
                      :retina-optimized? true
                      :retina-quality 0.4
                      :width 184}}}})
