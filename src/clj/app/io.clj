(ns app.io 
  (:require [app.config :refer [input-zips-dir]]
            [app.macros :refer [->hash]]
            [clojure.java.io :as io]) 
  (:import [java.io BufferedReader InputStreamReader]
           [java.util.zip ZipInputStream]))

;; (set! *warn-on-reflection* true)

(defn get-zips []
  (->> (io/file input-zips-dir)
       (file-seq)
       (filter #(.isFile %))
       (mapv #(.getPath %))))

(defn zip-file->line-seq [zip-file]
  (let [input-stream (io/input-stream zip-file)
        zip-input-stream (new ZipInputStream input-stream)
        entry (.getNextEntry zip-input-stream)
        filename (.getName entry)
        lines (-> (InputStreamReader. zip-input-stream)
                  (BufferedReader. (* 1024 1024 32))
                  (line-seq))]
    (->hash input-stream zip-input-stream filename lines)))
