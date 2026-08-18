(ns bb.chromedriver
  "Keeps the chromedriver mise uses in sync with the locally installed Chrome.

  ChromeDriver only refuses to talk to a Chrome build with a *different major
  version* (see the 'This version of ChromeDriver only supports Chrome version
  X' error) - so pinning is done per Chrome major version, matched against the
  newest published chromedriver release for that major. This runs on every
  `bb dev` boot so it self-heals across machines/Chrome auto-updates instead of
  relying on a version committed to the repo."
  (:require
   [babashka.process :refer [shell]]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def chrome-plist "/Applications/Google Chrome.app/Contents/Info.plist")

(defn detect-chrome-major []
  (when (.exists (io/file chrome-plist))
    (-> (shell {:out :string} "defaults" "read"
               "/Applications/Google Chrome.app/Contents/Info"
               "CFBundleShortVersionString")
        :out
        str/trim
        (str/split #"\.")
        first)))

(defn current-chromedriver-major []
  (try
    (-> (shell {:out :string :continue true} "mise" "current" "chromedriver")
        :out
        str/trim
        not-empty
        (some-> (str/split #"\.") first))
    (catch Exception _ nil)))

(defn latest-for-major [major]
  (->> (shell {:out :string} "mise" "ls-remote" "chromedriver")
       :out
       str/split-lines
       (filter #(str/starts-with? % (str major ".")))
       (keep (fn [v] (let [parts (mapv parse-long (str/split v #"\."))]
                       (when (= 4 (count parts)) [parts v]))))
       (sort-by first)
       last
       second))

(defn ensure-matching-chromedriver! []
  (if-let [major (detect-chrome-major)]
    (if (= major (current-chromedriver-major))
      (println "[chromedriver]" (str "already matches Chrome " major ".x"))
      (if-let [target (latest-for-major major)]
        (do
          (println "[chromedriver]" (str "Chrome is " major ".x, pinning chromedriver@" target))
          (shell "mise" "use" (str "chromedriver@" target)))
        (println "[chromedriver] WARN: no chromedriver release found for Chrome major" major ", leaving as-is")))
    (println "[chromedriver] Google Chrome not found at" chrome-plist "- skipping")))
