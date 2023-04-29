(ns macros)

(defn args [& xs]
  (let [vars (-> xs next next)]
    `(clojure.core/assoc ~'args :dummy (clojure.core/vector ~@vars))))
