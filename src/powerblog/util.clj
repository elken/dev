(ns powerblog.util
  (:import
   [java.time LocalDate]
   [java.time.format DateTimeFormatter]))

(defn ordinal-suffix [day]
  (cond
    (or (= day 11) (= day 12) (= day 13)) "th"
    (= (mod day 10) 1) "st"
    (= (mod day 10) 2) "nd"
    (= (mod day 10) 3) "rd"
    :else "th"))

(defn format-date [date-string]
  (let [parsed-date (LocalDate/parse date-string)
        day (.getDayOfMonth parsed-date)
        month (.format parsed-date (DateTimeFormatter/ofPattern "MMMM"))
        year (.getYear parsed-date)]
    (str day (ordinal-suffix day) " " month " " year)))
