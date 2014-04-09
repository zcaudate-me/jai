(ns juy.match
  (:require [rewrite-clj.zip :as z]
            [hara.common.checks :refer [hash-map?]]
            [juy.match.template :refer [match-template]]))
            
(defrecord Matcher [fn]
  clojure.lang.IFn
  (invoke [this node]
    ((:fn this) node)))

(defn matcher? [x]
  (instance? Matcher x))

(defn matches? [node template]
  (cond (fn? template) (template (z/sexpr node))
        (matcher? template) ((:fn template) node)
        (coll? template) (match-template (z/sexpr node))
        :else (= template (z/sexpr node))))

(defn p-symbol [template]
  (Matcher. (fn [node]
              (and (-> node z/tag (= :list))
                   (-> node z/down z/value (= template))))))

(defn p-type [template]
  (Matcher. (fn [node]
              (-> node z/tag (= template)))))

(defn p-is [template]
  (Matcher. (fn [node]
              (-> node (matches? template)))))

(defn p-left [template]
  (Matcher. (fn [node]
              (if node
                (-> node z/left (matches? template))))))

(defn p-right [template]
  (Matcher. (fn [node]
              (if node
                (-> node z/right (matches? template))))))

(defn p-contains [template]
  (Matcher. (fn [node]
              (if-let [chd (z/down node)]
                (->> chd
                     (iterate z/right)
                     (take-while identity)
                     (map #(matches? % template))
                     (some identity))))))

(defn p-and [& matchers]
  (Matcher. (fn [node]
              (->> (map #(%  node)  matchers)
                   (every? true? )))))

(defn p-or [& matchers]
  (Matcher. (fn [node]
              (->> (map #(%  node)  matchers)
                   (some true? )))))


(defn compile-matcher [template]
  (cond (symbol? template)   (p-symbol template)
        (list? template)     (p-is template)
        (hash-map? template)
        (apply p-and
               (map (fn [[k v]]
                      (condp = k
                        :type  (p-type v)
                        :is    (p-is v)
                        :right (p-right v)
                        :left  (p-left v)
                        :contains (p-contains v)))
                    template))))