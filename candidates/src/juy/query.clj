(ns gia.query
  (:require [rewrite-clj.zip :as z]
            [gia.query.walk  :as walk]
            [gia.query.match :as match]
            [gia.query.path.classify :as classify]
            [gia.query.path :as path]))

(defn match [path]
  (-> path
      (classify/classify)
      (path/compile-map)
      (match/compile-matcher)))

(defn select
  "(map z/sexpr
       (select (z/of-file \"src/gia/query/pattern/fn.clj\")
               '[defmethod :| {:left {:left {:left defmethod}}
                               :is ^:% vector?}]))
  => '([pat] [pat ocr] [a b])"
  {:added "0.1"}
  [zloc path]
  (let [atm  (atom [])]
    (walk/matchwalk zloc
                    [(match path)]
                    (fn [zloc]
                      (swap! atm conj zloc)
                      zloc))
    @atm))

(defn modify [zloc path func & args]
  (walk/matchwalk zloc
                  [(match path)]
                  (fn [zloc]
                    (apply func zloc args))))

(defn $* [context path & args]
  (let [context (cond (string? context)
                      (z/of-file context)

                      (map? context)
                      (cond (:file context)
                            (z/of-file (:file context))

                            (:code context)
                            (z/of-string (:code context))

                            :else (throw (ex-info "keys can only be either :file or :code" context)))
                      :else (throw (ex-info "context can only be a string or map" {:value context})))]
    (cond (empty? args)
          (->> (select context path)
               (map z/sexpr))

          :else (apply modify context path args))))

(defmacro $ [context path & args]
  `($* ~context (quote ~path) ~@args))

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
  #_($ "src/gia/query/match.clj"
       [defmethod :> ]
       )


  (->> (gia "src/gia/query/pattern/fn.clj" [{:form 'catch
                                             :ancestor 'defmethod}])
       

       (map z/sexpr))

  (->> (gia "src/gia/query/pattern/fn.clj" ['(defmethod _ [_ _] & _)
                                            
                                            ])
       ;;(map root-sexp)

       (map z/sexpr))

  (->> (gia "src/gia/query/pattern/fn.clj" ['(defmethod & _)
                                            list?
                                            ])
       ;;(map root-sexp)

       (map z/sexpr))

  (->> (gia "src/gia/query/match.clj" ['(defmethod _ [_ _] & _)
                                       #{vector? symbol?}
                                       ])
       ;;(map root-sexp)

       (map z/sexpr))
  (comment


    (>pst))
  )
