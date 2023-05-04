(ns app.prices
  (:require [app.config :refer [output-dir]]
            [app.io :refer [get-zips zip-file->line-seq]]
            [app.macros :refer [->hash field]]
            [app.s3]
            [app.utils :refer [line->token-array path->year+month
                               timestamp->oa-date]]
            [clj-async-profiler.core :as prof]
            [clojure.java.io :as io]
            [com.climate.claypoole :as cp]
            [taoensso.timbre :refer [debug]])
(:import [java.util HashMap]
         [java.util.zip ZipInputStream]
         [org.agrona.concurrent UnsafeBuffer]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(set! *warn-on-reflection* true)

(defn tokens->map-entry [^"[Ljava.lang.String;" tokens]
  [(field tokens "quote_datetime")
   (field tokens "active_underlying_price")])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def zero-float (float 0.0))

(defn convert-zip [{:as args :keys [root]} zip-path]
  (println (format "processing %s" zip-path))
  (let [[year month] (path->year+month zip-path)
        {:keys [^ZipInputStream zip-input-stream lines]} (zip-file->line-seq zip-path)
        xf (comp (map line->token-array)
                 (drop 1)
                 (map tokens->map-entry))
        prices (->> (eduction xf lines)
                    (reduce (fn [^HashMap hm
                                 [qdt price]]
                              (.put hm qdt price)
                              hm)
                            (new java.util.HashMap 8192)))]
    (.close zip-input-stream)
    (let [path (format "%s/prices/%s_%04d-%02d.t6" output-dir root year month)
          out (io/output-stream path)]
      (doseq [[t p] prices]
        (let [arr (byte-array (* 8 4))
              buf (new UnsafeBuffer arr)
              f (Float/parseFloat p)
              high f
              low f
              open f
              close f
              value zero-float
              vol zero-float]
          (.putDouble buf (* 0 4) (timestamp->oa-date t))
          (.putFloat buf (* 2 4) high)
          (.putFloat buf (* 3 4) low)
          (.putFloat buf (* 4 4) open)
          (.putFloat buf (* 5 4) close)
          (.putFloat buf (* 6 4) value)
          (.putFloat buf (* 7 4) vol)
          (.write out arr)))
      (.close out)
      path)))


(defn glue-files-together [{:keys [root]} paths]
  (let [output-path (format "%s/prices/%s.t6" output-dir root)]
    (with-open [o (io/output-stream (io/file output-path))]
      (doseq [p paths]
        (io/copy (io/file p) o)))
    output-path))


(defn run []
  (let [root "SPXW"
        args (->hash root)]
    (->> (get-zips)
         (sort)
        ;;  (take 1)
         (cp/pmap 8 (partial convert-zip args))
         ((partial glue-files-together args))
         (debug))))

(defn profiled-run [] (prof/profile (run)))
