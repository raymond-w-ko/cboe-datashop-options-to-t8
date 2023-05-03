(ns app.utils 
  (:require [app.macros :refer [field]]) 
  (:import app.Utils))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn path->year+month [path]
  (let [[_ year-str month-str] (re-find #".+(\d\d\d\d)-(\d\d)\.zip" path)
        year (Integer/parseInt year-str)
        month (Integer/parseInt month-str)]
    [year month]))

(defn timestamp->oa-date [ts]
  (Utils/convertQuoteDateTimeToOADate ts))

(defn exp-date->int [exp-date]
  (Utils/ExpDateToInt exp-date))

(defn dte [quote-datetime exp-date]
  (Utils/DTE quote-datetime exp-date))

(defn line->token-array
  [^String line]
  (.split line ","))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn is-spxw-root?
  [^"[Ljava.lang.String;" line]
  (= "SPXW" (field line "root")))

(defn gen-lteq-dte-filter?
  [target-dte]
  (fn [^"[Ljava.lang.String;" tokens]
    (let [quote-date (field tokens "quote_datetime")
          exp-date (field tokens "expiration")
          dte (dte quote-date exp-date)]
      (<= dte target-dte))))

(defn gen-lte-delta-filter?
  [target-delta]
  (fn [^"[Ljava.lang.String;" tokens]
    (let [delta (Float/parseFloat (field tokens "delta"))]
      (<= (abs delta) target-delta))))
