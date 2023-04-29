(ns user.repl 
  (:require app.io
            [app.s3 :as s3]))

(defn test-s3-zip-reading []
  (let [zip-object-key (->> (s3/get-input-zip-object-keys)
                            (first))
        {:keys [lines aws-response zip-input-stream]}
        (s3/zip-object-key->line-seq zip-object-key)]
    (dorun (map println (take 10 lines)))
    (.close zip-input-stream)
    (.abort aws-response)))

(defn test-local-zip-reading []
  (let [zip-file (->> (app.io/get-zips)
                      (first))
        {:keys [lines zip-input-stream]}
        (app.io/zip-file->line-seq zip-file)]
    (dorun (map println (take 10 lines)))
    (.close zip-input-stream)))
