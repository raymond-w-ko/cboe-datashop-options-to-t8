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
