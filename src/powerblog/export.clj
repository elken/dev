(ns powerblog.export
  (:require
   [clojure.core.async :refer [put!]]
   [powerblog.core :as blog]
   [powerpack.errors :as errors]
   [powerpack.export :as export]
[powerpack.dev :as dev]
   powerpack.ingest
   [powerpack.logger :as logger]))

;; TODO Remove after https://github.com/cjohansen/powerpack/issues/5
(defn call-ingest-callback [powerpack opt results]
  (try
    (let [on-ingested (:powerpack/on-ingested powerpack)]
      (when (ifn? on-ingested)
        (logger/debug "calling callback")
        (let [result (on-ingested powerpack results)]
          (logger/debug "got result" result)
          result)))
    (catch Exception e
      (logger/debug "got an exception" e)
      (->> {:kind ::callback
            :id [::callback]
            :message "Encountered an exception while calling your `on-ingested` hook, please investigate."
            :exception e}
           (errors/report-error opt))))
  (when-let [ch (-> opt :app-events :ch)]
    (logger/debug "Emit app-event :powerpack/ingested-content")
    (put! ch {:kind :powerpack/ingested-content
              :action "reload"})))

(alter-var-root #'powerpack.ingest/call-ingest-callback (fn [f]
                                                          call-ingest-callback))

(defn ^:export export! [& args]
  (-> blog/config
      export/export!))
