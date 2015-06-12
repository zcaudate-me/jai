(ns jai.match.pattern
  (:require  [hara.common.checks :refer [hash-map? lazy-seq?]]
             [jai.match regex fn
              [actual :as actual]
              [eval :as eval]
              [optional :as optional]]
             [jai.common :as common]
             [clojure.core.match :as match]
             [clojure.walk :as walk]))

(defn transform-pattern
  {:added "0.1"}
  [template]
  (cond (:& (meta template))       (actual/actual-pattern template)
        (:% (meta template))       (eval/eval-pattern template)
        (#{'(quote &)
           '(quote _)} template)    template
        (or (lazy-seq? template)
            (list? template))      (cond (empty? template)
                                         (actual/actual-pattern template)

                                         :else
                                         (list (apply list (map transform-pattern template)) :seq))    
        (#{'& '_} template)        template
        (vector? template)         (vec (map transform-pattern template))
        (set? template)            (let [pats (map transform-pattern template)
                                         pats (if (empty? pats) [#'common/none] pats)]
                                     (apply list :or pats))
        (hash-map? template)  (->> (map (fn [[k v]]
                                      [(transform-pattern k) (transform-pattern v)]) template)
                                   (into {}))
        (symbol? template)    (list 'quote template)
        :else template))

(defn pattern-form
  [sym template]
  (let [clauses [[(transform-pattern template)] true :else false]]
    (match/clj-form [sym] clauses)))

(defn pattern-single-fn [template]
  (let [sym        (gensym)
        match-form (pattern-form sym template)
        all-fn    `(fn [form#]
                     (let [~sym form#]
                       ~match-form))]
    (eval all-fn)))

(defn pattern-matches
  "pattern
  ((pattern-matches ()) ())
  => '(())
  
  ((pattern-matches '(^:% symbol? ^:? (+ 1 _ ^:? _))) '(+ (+ 1 2 3)))
  => '((^{:% true} symbol? ^{:? 0} (+ 1 _ ^{:? 1} _)))"
  {:added "0.2"}
  [template]
  (let [all-fns (->> template
                     (optional/pattern-seq)
                     (mapv (juxt identity pattern-single-fn)))]
    (fn [form]
      (or (mapcat (fn [[template f]]
                    (if (f form) [template])) all-fns)
          []))))

(defn pattern-fn [template]
  (fn [value]
    (-> ((pattern-matches template) value)
        empty?
        not)))
