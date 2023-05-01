(ns user.repl 
  (:require app.io
            [app.macros :as macros]
            [app.s3 :as s3]
            [clojure.java.io :as io]
            [clojure.walk :as walk]))

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

(defn test-macros []
  (prn (walk/macroexpand-all '(app.macros/bb)))
  (prn (walk/macroexpand-all '(app.macros/bb 1)))
  (prn (walk/macroexpand-all '(app.macros/bb (println "hi"))))
  (prn (walk/macroexpand-all '(app.macros/bb
                               a "hello"
                               (println a))))
  (prn (walk/macroexpand-all '(app.macros/bb
                               a "hello"
                               b "world"
                               (println a b))))
  (prn (walk/macroexpand-all '(app.macros/bb
                               a "hello"
                               b "world"
                               :when a (println "short circuit")
                               (println a b))))
  (prn (walk/macroexpand-all '(app.macros/bb
                               a "hello"
                               b "world"
                               :when a (println "short circuit")
                               c "!"
                               (println a b c d))))
  (app.macros/bb)
  (app.macros/bb
   a "hello"
   b false
   :when b (println "short circuit")
   :do (println "do")
   c "!"
   :with-open e (io/writer "test.txt")
   :do (println e)
   (println a b c))
  true)
