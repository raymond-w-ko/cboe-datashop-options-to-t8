(ns app.core
  (:require [app.config :refer [output-dir]]
            [app.io :refer [get-zips zip-file->line-seq]]
            [app.macros :refer [field]]
            [app.s3]
            [clojure.java.io :as io]
            [com.climate.claypoole :as cp])
  (:import app.Utils
           [java.io OutputStream]
           [java.util.zip ZipInputStream]
           [org.agrona.concurrent UnsafeBuffer]))

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

(defn tokens->byte-array
  [^"[Ljava.lang.String;" tokens]
  (let [arr (byte-array (+ (* 1 8) (* 8 4)))
        buf (new UnsafeBuffer arr)

        quote-datetime (timestamp->oa-date (field tokens "quote_datetime"))
        bid (Float/parseFloat (field tokens "bid"))
        ask (Float/parseFloat (field tokens "ask"))
        delta (Float/parseFloat (field tokens "delta"))
        implied-volatility (Float/parseFloat (field tokens "implied_volatility"))
        active-underlying-price (Float/parseFloat (field tokens "active_underlying_price"))
        strike (Float/parseFloat (field tokens "strike"))
        exp-date-int (exp-date->int (field tokens "expiration"))
        contract-type (-> (case (field tokens "option_type")
                            "C" 1
                            "P" 2)
                          int)]
    (.putDouble buf (* 0 4) quote-datetime)
    (.putFloat buf (* 2 4) bid)
    (.putFloat buf (* 3 4) ask)
    (.putFloat buf (* 4 4) delta)
    (.putFloat buf (* 5 4) implied-volatility)
    (.putFloat buf (* 6 4) active-underlying-price)
    (.putFloat buf (* 7 4) strike)
    (.putInt buf (* 8 4) exp-date-int)
    (.putInt buf (* 9 4) contract-type)
    arr))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn convert-zip [zip-path]
  (let [[year month] (path->year+month zip-path)
        output-path (format "%s/%04d-%02d.t8" output-dir year month)]
    (println (format "%s -> %s" zip-path output-path))
    (let [{:keys [^ZipInputStream zip-input-stream lines]} (zip-file->line-seq zip-path)
          is-right-dte? (gen-lteq-dte-filter? 9)
          is-right-delta? (gen-lte-delta-filter? 0.80)
          xf (comp (map line->token-array)
                   (drop 1)
                   (filter is-spxw-root?)
                   (filter is-right-dte?)
                   (filter is-right-delta?)
                   (map tokens->byte-array))
          out (io/file output-path)
          out-stream (io/output-stream out)]
      (->> (eduction xf lines)
           (reduce (fn [^OutputStream out-stream ^"[B" bytes]
                     (.write out-stream bytes)
                     out-stream)
                   out-stream))
      (.close out-stream)
      (.close zip-input-stream))))

(defn run []
  (let [is-modern-zip?
        (fn [path]
          (let [[year _month] (path->year+month path)]
            (<= 2018 year)))]
    (->> (get-zips)
         (filter is-modern-zip?)
         (sort)
         (take 1)
         (cp/pmap 4 convert-zip)
         (count))))

(defn -main
  "Entry point for the application."
  []
  (println "Hello World"))
