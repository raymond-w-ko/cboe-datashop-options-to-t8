(ns build
  (:require [clojure.tools.build.api :as b]))

(def basis (b/create-basis {:project "deps.edn"}))

(defn javac [_]
  (b/javac {:src-dirs ["src/jvm"]
            :class-dir "target/classes"
            :basis basis
            :javac-opts ["-Xlint:-options"]}))
