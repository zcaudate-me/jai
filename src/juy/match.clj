(ns juy.match
  (:require  [hara.common.checks :refer [hash-map?]]
             [clojure.core.match :refer [clj-form]]))

(defn transform-match [template]
  (cond (list? template)      (list (apply list (map transform-match template)) :seq)
        (:+ (meta template))  (eval template)
        (:- (meta template))  template
        ('#{& _} template)    template
        (vector? template)    (vec (map transform-match template))
        (set? template)       (set (map transform-match template))
        (hash-map? template)  (->> (map (fn [[k v]]
                                      [(transform-match k) (transform-match v)]) template)
                               (into {}))
        (symbol? template)    (list 'quote template)
        :else template))

(defrecord FnPattern [fn])

(defmethod clojure.core.match/emit-pattern clojure.lang.Fn
  [pat]
  (FnPattern. pat))

(defmethod clojure.core.match/to-source FnPattern
  [pat ocr]
  `(~(:fn pat) ~ocr))

(defmethod clojure.core.match/groupable? [FnPattern FnPattern]
  [a b]
  (let [ra (:fn a)
        rb (:fn b)]
    (and (= (.pattern ra) (.pattern rb))
         (= (.flags ra) (.flags rb)))))

(defrecord RegexPattern [regex])

(defmethod clojure.core.match/emit-pattern java.util.regex.Pattern
  [pat]
  (RegexPattern. pat))

(defmethod clojure.core.match/to-source RegexPattern
  [pat ocr]
  `(re-find ~(:regex pat) ~ocr))

(defmethod clojure.core.match/groupable? [RegexPattern RegexPattern]
  [a b]
  (let [ra (:regex a)
        rb (:regex b)]
    (and (= (.pattern ra) (.pattern rb))
         (= (.flags ra) (.flags rb)))))

(defn match-clauses [template]
  [[(transform-match template)] true :else false])

(defn match-template [form template]
  (let [clauses (match-clauses template)
        sym   (gensym)
        match-form (clojure.core.match/clj-form [sym] clauses)
        all    (list 'let [sym (list 'quote form)]
                     match-form)]
    (eval all)))
