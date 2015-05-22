(ns jai.query
  (:require [jai.match :as match]
            [jai.common :as common]
            [jai.query.compile :as compile]
            [jai.query.traverse :as traverse]
            [jai.query.walk :as walk]
            [rewrite-clj.zip :as source]))

(defn match
  "matches the source code
  (match (source/of-string \"(+ 1 1)\") '(symbol? _ _))
  => false
  
  (match (source/of-string \"(+ 1 1)\") '(^:% symbol? _ _))
  => true

  (match (source/of-string \"(+ 1 1)\") '(^:%- symbol? _ | _))
  => true

  (match (source/of-string \"(+ 1 1)\") '(^:%+ symbol? _ _))
  => false"
  {:added "0.2"}
  [zloc selector]
  (let [match-fn (-> selector
                     (compile/expand-all-metas)
                     (common/prepare-deletion)
                     (match/compile-matcher))]
    (try (match-fn zloc)
         (catch Throwable t false))))

(defn traverse
  "uses a pattern to traverse as well as to edit the form
  
  (source/sexpr
   (traverse (source/of-string \"^:a (+ () 2 3)\")
             '(+ () 2 3)))
  => '(+ () 2 3)
  
  (source/sexpr
   (traverse (source/of-string \"()\")
             '(^:&+ hello)))
  => '(hello)
  
  (source/sexpr
   (traverse (source/of-string \"()\")
             '(+ 1 2 3)))
  => throws
  
  (source/sexpr
   (traverse (source/of-string \"(defn hello \\\"world\\\" {:a 1} [])\")
             '(defn ^:% symbol? ^:?%- string? ^:?%- map? ^:% vector? & _)))
  => '(defn hello [])"
  {:added "0.2"}
  [zloc pattern]
  (let [pattern (compile/expand-all-metas pattern)]
    (:source (traverse/traverse zloc pattern))))

(defn select
  "selects all patterns from a starting point
  (map source/sexpr
   (select (source/of-string \"(defn hello [] (if (try))) (defn hello2 [] (if (try)))\")
           '[defn if try]))
  => '((defn hello  [] (if (try)))
       (defn hello2 [] (if (try))))"
  {:added "0.2"}
  [zloc selectors]
  (let [[match-map [cidx ctype cform]] (compile/prepare selectors)
        match-fn (match/compile-matcher match-map)]
    (let [atm  (atom [])]
      (walk/matchwalk zloc
                      [match-fn]
                      (fn [zloc]
                        (swap! atm conj 
                               (if (= :form ctype)
                                 (:source (traverse/traverse zloc cform))
                                 zloc))
                        zloc))
      @atm)))

(defn modify
  "modifies location given a function
  (source/root-string
   (modify (source/of-string \"^:a (defn hello3) (defn hello)\") ['(defn | _)]
           (fn [zloc]
             (source/insert-left zloc :hello))))
  => \"^:a (defn :hello hello3) (defn :hello hello)\""
  {:added "0.2"}
  [zloc selectors func]
  (let [[match-map [cidx ctype cform]] (compile/prepare selectors)
        match-fn (match/compile-matcher match-map)]
    (walk/matchwalk zloc
                    [match-fn]
                    (fn [zloc]
                      (if (= :form ctype)
                        (let [{:keys [level source]} (traverse/traverse zloc cform)
                              nsource (func source)]
                          
                          (if (or (nil? level) (= level 0))
                            nsource
                            (nth (iterate source/up nsource) level)))
                        (func zloc))))))

(defn context-zloc [context]
  (cond (string? context)
        (query-context (source/of-file context))

        (vector? context) context
        
        (map? context)
        (-> (cond (:source context)
                  (:source context)
                  
                  (:file context)
                  (source/of-file (:file context))
                  

                  (:string context)
                  (source/of-string (:string context))

                  :else (throw (ex-info "keys can only be either :file or :string" context))))
        :else (throw (ex-info "context can only be a string or map" {:value context}))))

(defn wrap-vec [f]
  (fn [res opts]
    (if (vector? res)
      (mapv #(f % opts) res)
      (f res opts))))

(defn wrap-return [f]
  (fn [res {:keys [return] :as opts}]
    (case return
      :string (source/string (f res opts))
      :zipper (f res opts)
      :sexpr  (source/sexpr (f res opts)))))

(defn $*
  [context path & [func? opts?]]
  (let [zloc (context-zloc context)
        [func opts] (cond (nil? func?) [nil opts?]
                          (map? func?) [nil func?]
                          :else [func? opts?])
        results     (cond func
                          (modify zloc path func)
                          
                          :else
                          (select zloc path))
        opts         (merge {:return (if func :zipper :sexpr)} opts)]
    ((-> (fn [res opts] res)
          wrap-return
          wrap-vec) results opts)))

(defmacro $
  "select and manipulation of clojure source code
  
  ($ {:string \"(defn hello1) (defn hello2)\"} [(defn _ ^:%+ (keyword \"oeuoeuoe\") )])
  => '((defn hello1 :oeuoeuoe) (defn hello2 :oeuoeuoe))

  ($ {:string \"(defn hello1) (defn hello2)\"} [(defn _ | ^:%+ (keyword \"oeuoeuoe\") )])
  => '(:oeuoeuoe :oeuoeuoe)"
  {:added "0.2"}
  [context path & args]
  `($* ~context (quote ~path) ~@args))
