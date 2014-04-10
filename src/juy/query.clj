(ns juy.query
  (:require [rewrite-clj.zip :as z]
            [juy.query.walk :refer [matchwalk postwalk prewalk levelwalk]]
            [juy.query.match :refer [compile-matcher]]))

(defn $ [])

(defn $ [])

(defn $* [])

(defn juy
  ([context templates & [update-fn? & modifiers]]
     (let [zloc (z/of-file context)
           [update-fn modifiers]
           (if (fn? update-fn?)
             [update-fn? modifiers]
             [nil modifiers])
           atm  (atom [])]
        (levelwalk zloc
                        (map compile-matcher templates)
                        (fn [zloc]
                          (let [nloc (if update-fn
                                       (update-fn zloc)
                                       zloc)]
                            (swap! atm conj nloc)
                            nloc)))
        @atm)))

(defn root-sexp [zloc]
  (if-let [nloc (z/up zloc)]
    (condp = (z/tag nloc)
      :forms   zloc
      :branch? zloc
      (recur nloc))
    zloc))

(defn parent-sexp [zloc]
  (if-let [nloc (z/up zloc)]
    (condp = (z/tag nloc)
      :forms   zloc
      :branch? zloc
      nloc)
    zloc))

($ "src/juy/query/match.clj"
   [defmethod :> ]
   )


(->> (juy "src/juy/query/match.clj" ['(defmethod _ [_ _] & _)
                                     #{vector? symbol?}
                                     ])
     ;;(map root-sexp)

     (map z/sexpr))
(comment


    (>pst))
