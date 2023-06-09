(ns app.options
  (:require [app.config :refer [output-dir]]
            [app.io :refer [get-zips zip-file->line-seq]]
            [app.macros :refer [->hash field]]
            [app.s3]
            [app.utils :refer [exp-date->int gen-lte-delta-filter?
                               gen-lteq-dte-filter? is-spxw-root? line->token-array
                               path->year+month timestamp->oa-date]]
            [clj-async-profiler.core :as prof]
            [clojure.java.io :as io]
            [com.climate.claypoole :as cp]
            [taoensso.timbre :refer [debug]]) 
  (:import [java.io OutputStream]
           [java.util.zip ZipInputStream]
           [org.agrona.concurrent UnsafeBuffer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(set! *warn-on-reflection* true)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tokens->byte-array
  [^"[Ljava.lang.String;" tokens]
  (let [arr (byte-array (+ (* 1 8) (* 8 4)))
        buf (new UnsafeBuffer arr)

        quote-date (-> (field tokens "quote_datetime")
                       (subs 0 10))
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

    ;; not a mistake, it really is ask/bid in *.t8 files
    (.putFloat buf (* 2 4) ask)
    (.putFloat buf (* 3 4) bid)

    (.putFloat buf (* 4 4) delta)
    (.putFloat buf (* 5 4) implied-volatility)
    (.putFloat buf (* 6 4) active-underlying-price)
    (.putFloat buf (* 7 4) strike)
    (.putInt buf (* 8 4) exp-date-int)
    (.putInt buf (* 9 4) contract-type)
    [quote-date arr]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn quote-writer [{:keys [root]}
                    outs
                    [^String date ^"[B" bytes]]
  (let [has-open-out (contains? outs date)
        ^OutputStream out-stream
        (if has-open-out
          (get outs date)
          (let [path (format "%s/%s_%s.t8" output-dir root date)]
            (println "creating " path)
            (io/output-stream (io/file path))))]
    (.write out-stream bytes)
    (if has-open-out
      outs
      (assoc outs date out-stream))))

(defn convert-zip [args zip-path]
  (println (format "processing %s" zip-path))
  (let [{:keys [^ZipInputStream zip-input-stream lines]} (zip-file->line-seq zip-path)
        is-right-dte? (gen-lteq-dte-filter? 3)
        is-right-delta? (gen-lte-delta-filter? 0.80)
        xf (comp (map line->token-array)
                 (drop 1)
                 (filter is-spxw-root?)
                 (filter is-right-dte?)
                 (filter is-right-delta?)
                 (map tokens->byte-array))
        outs (->> (eduction xf lines)
                  (reduce (partial quote-writer args) {}))]
    (.close zip-input-stream)
    (doseq [[_ ^OutputStream out] outs]
      (debug "closing" out)
      (.close out))
    (keys outs)))

(defn run []
  (let [root "SPXW"
        args (->hash root)

        is-modern-zip?
        (fn [path]
          (let [[year _month] (path->year+month path)]
            (<= 2018 year)))]
    (->> (get-zips)
         (filter is-modern-zip?)
         (sort)
         (cp/pmap 8 (partial convert-zip args))
         (debug))))

(defn profiled-run [] (prof/profile (run)))
