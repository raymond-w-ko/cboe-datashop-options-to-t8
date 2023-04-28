(ns app.s3 
  (:import [software.amazon.awssdk.auth.credentials StaticCredentialsProvider AwsBasicCredentials]
           [software.amazon.awssdk.regions Region]
           [software.amazon.awssdk.services.s3 S3Client]
           [software.amazon.awssdk.services.s3.model GetObjectRequest ListObjectsV2Request])
  (:require [app.config :refer [aws-access-key-id aws-secret-access-key
                                input-zips-bucket input-zips-prefix]]))

(def credentials
  (StaticCredentialsProvider/create
   (AwsBasicCredentials/create aws-access-key-id aws-secret-access-key)))

(def s3 (-> (S3Client/builder)
            (.region Region/US_EAST_1)
            (.credentialsProvider credentials)
            (.build)))

(defn get-input-zips
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
