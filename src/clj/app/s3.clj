(ns app.s3 
  (:require [app.config :refer [aws-access-key-id aws-secret-access-key
                                input-zips-bucket input-zips-prefix]]
            [app.macros :as mac :refer [->hash]])
  (:import [java.io BufferedReader InputStreamReader]
           [java.util.zip ZipInputStream]
           [software.amazon.awssdk.auth.credentials AwsBasicCredentials StaticCredentialsProvider]
           [software.amazon.awssdk.regions Region]
           [software.amazon.awssdk.services.s3 S3Client]
           [software.amazon.awssdk.services.s3.model GetObjectRequest ListObjectsV2Request]))

(def credentials
  (StaticCredentialsProvider/create
   (AwsBasicCredentials/create aws-access-key-id aws-secret-access-key)))

(def s3 (-> (S3Client/builder)
            (.region Region/US_EAST_1)
            (.credentialsProvider credentials)
            (.build)))

(defn get-input-zip-object-keys
  "Returns a vector of object keys for the input zips."
  []
  (loop [req (-> (ListObjectsV2Request/builder)
               (.bucket input-zips-bucket)
               (.prefix input-zips-prefix)
               (.build))
         output []]
    (let [resp (.listObjectsV2 s3 req)
          xs (mapv #(.key %) (-> resp .contents))]
      (let [token (.nextContinuationToken resp)]
        (if-not token
          (into output xs)
          (recur (-> (.toBuilder req)
                     (.continuationToken token)
                     (.build))
                 (into output xs)))))))

(defn get-object [object-key]
  (let [req (-> (GetObjectRequest/builder)
                (.bucket input-zips-bucket)
                (.key object-key)
                (.build))
        aws-response (.getObject s3 req)]
    (->hash aws-response)))

(defn ^ZipInputStream zip-object-key->input-stream
  [zip-object-key]
  (let [{:as args :keys [aws-response]} (get-object zip-object-key)
        zip-input-stream (new ZipInputStream aws-response)]
    (mac/args zip-input-stream)))

(defn zip-object-key->line-seq [zip-object-key]
  (let [{:as args :keys [zip-input-stream]} (zip-object-key->input-stream zip-object-key)
        entry (.getNextEntry zip-input-stream)
        filename (.getName entry)
        lines (-> (InputStreamReader. zip-input-stream)
                  (BufferedReader. (* 1024 1024 32))
                  (line-seq))]
    (mac/args filename lines)))

