(ns juy.match
  (:require [rewrite-clj.zip :as z]
            [hara.common.checks :refer [hash-map? regex?]]
            [juy.match.template :refer [match-fn]]))

(defrecord Matcher [fn]
  clojure.lang.IFn
  (invoke [this zloc]
    (fn zloc)))

(defn matcher [f]
  (Matcher. f))

(defn matcher? [x]
  (instance? Matcher x))

(defn p-symbol [template]
  (Matcher. (fn [zloc]
              (and (-> zloc z/tag (= :list))
                   (-> zloc z/down z/value (= template))))))

(defn p-type [template]
  (Matcher. (fn [zloc]
              (-> zloc z/tag (= template)))))

(defn p-is
  "hello world"
  [template]
  (Matcher. (fn [zloc]
              (if (fn? template)
                (-> zloc z/sexpr template)
                (-> zloc z/sexpr (= template))))))

(defn p-matches [template]
  (Matcher. (fn [zloc]
              (let [mf (match-fn template)]
                (-> zloc z/sexpr mf)))))

(defn p-and [& matchers]
  (Matcher. (fn [zloc]
              (->> (map (fn [m] (m zloc)) matchers)
                   (every? true?)))))

(defn p-or [& matchers]
  (Matcher. (fn [zloc]
              (->> (map (fn [m] (m zloc)) matchers)
                   (some true?)))))

(declare p-contains p-right p-left)

(defn compile-matcher [template]
  (cond (:- (meta template)) (p-is template)
        (symbol? template)   (p-symbol template)
        (fn? template)       (p-is template)
        (list? template)     (p-matches template)
        (regex? template)    (p-matches template)
        (vector? template)   (apply p-and (map compile-matcher template))
        (set? template)      (apply p-or (map compile-matcher template))
        (hash-map? template)
        (apply p-and
               (map (fn [[k v]]
                      (condp = k
                        :type     (p-type v)
                        :matches  (p-matches v)
                        :is       (p-is v)
                        :contains (p-contains v)
                        ;;:right    (p-right v)
                        ;;:left     (p-left v)
                        ))
                    template))))

(defn nested-compile-matcher [template]
  (let [template (if (and (or (hash-map? template)
                              (set? template)
                              (list? template)
                              (vector? template))
                          (-> (meta template) :- not))
                   (compile-matcher template)
                   (p-is template))]))

(defn p-contains [template]
  (Matcher. (fn [zloc]
              (let [mf (nested-compile-matcher template)]
                (if-let [chd (z/down zloc)]
                  (->> chd
                       (iterate z/right)
                       (take-while identity)
                       (map mf)
                       (some identity)))))))
