(ns hooks.binding-block
  (:require [clj-kondo.hooks-api :as api :refer [list-node vector-node map-node keyword-node
                                                 token-node string-node
                                                 keyword-node?]]))

(defn dump [node] (prn (api/sexpr node)))

(defn rewrite [forms]
  ;; (prn (first forms))
  (cond
    ;; no form is nil
    (= (count forms) 0) (token-node 'nil)

    ;; one thing remaining is the return value
    (= (count forms) 1)
    (first forms)

    ;; keyword special detected
    (keyword-node? (first forms))
    (let [[_ x & xs] forms
          [k a b & cs] forms]
      (case (:k k)
        (:do) (list-node
               (list (token-node 'do)
                     x
                     (rewrite xs)))
        (:when) (list-node
                 (list (token-node 'if) a
                       b
                       (rewrite cs)))
        (:with-open) (list-node
                      (list (token-node 'clojure.core/with-open)
                            (vector-node [a b])
                            (rewrite cs)))
        (token-node 'nil)))

    ;; collect let forms and recurse
    :else
    (let [[let-forms remaining-forms]
          (loop [let-forms []
                 remaining-forms forms]
            (if (<= (count remaining-forms) 1)
              [let-forms remaining-forms]
              (let [[a b & xs] remaining-forms]
                (if-not (keyword-node? a)
                  (recur (conj let-forms a b)
                         xs)
                  [let-forms remaining-forms]))))]
      (if (< 0 (count let-forms))
        (list-node
         (list (token-node 'let)
               (vector-node let-forms)
               (rewrite remaining-forms)))
        (rewrite remaining-forms)))))

(defn binding-block [{:as m :keys [node cljc lang filename config ns context]}]
  (let [final-node (rewrite (-> node :children rest))]
    ;; (dump final-node)
    {:node final-node}))
