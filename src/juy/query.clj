(ns juy.query
  (:require [rewrite-clj.zip :as z]
            [juy.query.walk  :as walk]
            [juy.query.match :as match]
            [juy.query.path :as path]))

(defn select [zloc path]
  (let [atm  (atom [])]
    (walk/matchwalk zloc
                    [(-> path
                         (path/compile-path)
                         (match/compile-matcher))]
                    (fn [zloc]
                      (swap! atm conj zloc)
                      zloc))
    @atm))

(defn modify [zloc path func & args]
  (walk/matchwalk zloc
                  [(-> path
                       (path/compile-path)
                       (match/compile-matcher))]
                  
                  (fn [zloc]
                    (apply func zloc args))))

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

(comment
  #_($ "src/juy/query/match.clj"
       [defmethod :> ]
       )


  (->> (juy "src/juy/query/pattern/fn.clj" [{:form 'catch
                                             :ancestor 'defmethod}])
       

       (map z/sexpr))

  (->> (juy "src/juy/query/pattern/fn.clj" ['(defmethod _ [_ _] & _)
                                            
                                            ])
       ;;(map root-sexp)

       (map z/sexpr))

  (->> (juy "src/juy/query/pattern/fn.clj" ['(defmethod & _)
                                            list?
                                            ])
       ;;(map root-sexp)

       (map z/sexpr))

  (->> (juy "src/juy/query/match.clj" ['(defmethod _ [_ _] & _)
                                       #{vector? symbol?}
                                       ])
       ;;(map root-sexp)

       (map z/sexpr))
  (comment


    (>pst))
  )
