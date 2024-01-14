(ns powerblog.export
  (:require [powerblog.core :as blog]
            [powerpack.export :as export]))

(defn ^:export export! [& args]
  (-> blog/config
      export/export!))
