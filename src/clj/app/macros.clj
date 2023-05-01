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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmacro binding-block [& body]
  (cond
    ;; no form is nil
    (= (count body) 0) nil

    ;; one thing remaining is the return value
    (= (count body) 1)
    (first body)

    ;; keyword special detected
    (keyword? (first body))
    (let [[_ x & xs] body
          [k a b & cs] body]
      (case k
        :do `(do ~x
                 (binding-block ~@xs))
        :when `(if ~a
                 ~b
                 (binding-block ~@cs))
        :with-open `(clojure.core/with-open [~a ~b]
                      (binding-block ~@cs))))

    ;; collect let forms and recurse
    :else
    (let [[let-forms remaining-body]
          (loop [let-forms []
                 remaining-body body]
            (if (<= (count remaining-body) 1)
              [let-forms remaining-body]
              (let [[a b & xs] remaining-body]
                (if-not (keyword? a)
                  (recur (conj let-forms a b)
                         xs)
                  [let-forms remaining-body]))))]
      (if (< 0 (count let-forms))
        `(let [~@let-forms]
           (binding-block ~@remaining-body))
        `(binding-block ~@remaining-body)))))

(defmacro bb [& body]
  `(binding-block ~@body))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def field-names
  ["underlying_symbol"
   "quote_datetime"
   "root"
   "expiration"
   "strike"
   "option_type"
   "open"
   "high"
   "low"
   "close"
   "trade_volume"
   "bid_size"
   "bid"
   "ask_size"
   "ask"
   "underlying_bid"
   "underlying_ask"
   "implied_underlying_price"
   "active_underlying_price"
   "implied_volatility"
   "delta"
   "gamma"
   "theta"
   "vega"
   "rho"
   "open_interest"])
(def field-name-indexes (into {} (map-indexed (fn [i x] [x i]) field-names)))

(defmacro field [arr k]
  (let [i (get field-name-indexes k)
        transformer (case k
                      `(identity))]
    `(-> (aget ~arr ~i)
         ~@transformer)))
