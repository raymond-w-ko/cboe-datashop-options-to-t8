(ns app.macros)

(defmacro ->hash [& vars]
  (list `zipmap
        (mapv keyword vars)
        (vec vars)))

(defmacro args
  "Converts (args a b c) -> (assoc args :a a :b b :c c)"
  [& vars]
  (let [xs (interleave (mapv keyword vars)
                       (vec vars))]
    `(assoc ~'args ~@xs)))
