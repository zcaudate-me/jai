(ns juy.query.pattern
  (:require  [hara.common.checks :refer [hash-map?]]
             [juy.query.pattern regex fn]
             [clojure.core.match :as match]
             [clojure.walk :as walk]))

(defn lazy-seq? [x] (instance? clojure.lang.LazySeq x))

(defn transform-pattern
  "turns a juy pattern into a core.match pattern
  (transform-pattern 'def)
  => '(quote def)
  
  (transform-pattern '(+ 1 2 3))
  => '(((quote +) 1 2 3) :seq)

  (transform-pattern '(^:% vector?))
  => (list (list vector?) :seq)
  
  (transform-pattern ''&)
  => '(quote &)

  
  (transform-pattern '&)
  => '&

  (transform-pattern '{^:% symbol? 1})
  => {symbol? 1}"
  {:added "0.1"}
  [template]
  (cond (#{'(quote &)
           '(quote _)} template)    template
        (or (lazy-seq? template)
            (list? template))      (list (apply list (map transform-pattern template)) :seq)
        (:% (meta template))       (eval template)
        (#{'& '_} template)        template
        (vector? template)         (vec (map transform-pattern template))
        (set? template)            (set (map transform-pattern template))
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

(defn- expand-meta
  [ele out]
  (let [mele (meta ele)]
    (cond (:%? mele)
          (expand-meta (with-meta ele (-> (dissoc mele :%?)
                                          (assoc :% true :? true)))
                       out)
          (:? mele)
          (do (swap! out update-in [:?] inc)
              (with-meta ele (assoc mele :? (:? @out))))

          :else ele)))

(defn- remove-element [ele combo]
  (if-let [num (-> ele meta :?)]
    (if (-> combo (bit-shift-right num) (mod 2) (= 0))
      ::null
      ele)
    ele))

(defn- remove-nulls [ele]
  (cond (list? ele) (filter #(not= ::null %) ele)
        (vector? ele) (filterv #(not= ::null %) ele)
        :else ele))

(defn pattern-seq [template]
  (let [out      (atom {:? -1})
        template (walk/postwalk #(expand-meta % out) template)
        combos   (range (bit-shift-left 1 (inc (:? @out))))]
    (for [combo combos]
      (->> template
           (walk/postwalk #(remove-element % combo))
           (walk/prewalk remove-nulls)))))

(defn pattern-fn [template]
  (let [all-fns (mapv pattern-single-fn (distinct (pattern-seq template)))]
    (fn [form]
      (or (some #(% form) all-fns)
          false))))
