(ns powerblog.pages
  (:require
   [powerblog.pages.about :as about]
   [powerblog.pages.article :as article]
   [powerblog.pages.blog-post :as blog-post]
   [powerblog.pages.feed :as feed]
   [powerblog.pages.frontpage :as frontpage]
   [powerblog.pages.tag :as tag]
   [powerblog.pages.webmanifest :as webmanifest]))

(defn render-page [context page]
  (case (:page/kind page)
    :page.kind/atom-feed (feed/render-atom-feed context)
    :page.kind/planetclojure-feed (feed/render-planetclojure-feed context)
    :page.kind/webmanifest (webmanifest/render-manifest context)
    :page.kind/about-page (about/render-page context page)
    :page.kind/frontpage (frontpage/render-page context page)
    :page.kind/blog-post (blog-post/render-page context page)
    :page.kind/tags-listing (tag/render-tags-page context page)
    :page.kind/article (article/render-page context page)
    :page.kind/tag (tag/render-tag-page context page)))
