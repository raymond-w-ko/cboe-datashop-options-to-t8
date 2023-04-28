(ns app.s3 
  (:import [software.amazon.awssdk.services.s3 S3Client]
           [software.amazon.awssdk.regions Region]
           [software.amazon.awssdk.auth.credentials StaticCredentialsProvider AwsBasicCredentials])
  (:require [app.config :refer [aws-access-key-id aws-secret-access-key]]))

(def credentials
  (StaticCredentialsProvider/create
   (AwsBasicCredentials/create aws-access-key-id aws-secret-access-key)))

(def s3 (-> (S3Client/builder)
            (.region Region/US_EAST_1)
            (.credentialsProvider credentials)
            (.build)))
