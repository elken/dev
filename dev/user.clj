(ns user
  (:require
   [portal.api :as inspect]
   [powerblog.core :as blog]
   [powerpack.dev :as dev]))

(defmethod dev/configure! :default []
  blog/config)

(defonce portal (inspect/open {:portal.colors/theme :portal.colors/nord}))
(add-tap #'portal.api/submit)

(dev/start)


(comment
  (inspect/clear)  ; Clear all values in the portal inspector window
  (inspect/close)  ; Close the inspector

  (require '[datomic.api :as d])
  (set! *print-namespace-maps* false)

  (def app (dev/get-app))
  (def db (d/db (:datomic/conn app)))

  (dev/start)
  (dev/stop)
  (dev/reset)

  (->> (d/entity db [:page/uri "/blog-posts/first-post/"])
       ;; :page/body
       (into {})
       ;; (.split "\\W+")
       ;; seq
       ;; count
       )

  (->> (d/q '[:find ?tx-time
              :where
              [(= :page/uri "/blog-posts/first-post/")]
              [?tx :db/txInstant ?tx-time]]
            db)
       sort
       second)

  (d/q '[:find [?tag ...]
         :where
         [_ :blog-post/tags ?tag]]
       db)

  )
