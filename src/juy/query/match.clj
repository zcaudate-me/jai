(ns juy.query.match
  (:require [rewrite-clj.zip :as z]
            [hara.common.checks :refer [hash-map? regex?]]
            [juy.query.pattern :refer [pattern-fn]]))

(defrecord Matcher [fn]
  clojure.lang.IFn
  (invoke [this zloc]
    (fn zloc)))

(defn matcher [f]
  (Matcher. f))

(defn matcher? [x]
  (instance? Matcher x))

(defn p-fn
  [template]
  (Matcher. (fn [zloc]
              (template zloc))))

(defn p-is
  [template]
  (Matcher. (fn [zloc]
              (if (fn? template)
                (-> zloc z/sexpr template)
                (-> zloc z/sexpr (= template))))))

(defn p-equal-loop [expr template]
  (and (= (meta expr) (meta template))
       (= (type expr) (type template))
       (cond (or (list? expr) (vector? expr))
             (and (= (count expr) (count template))
                  (every? true? (map p-equal-loop expr template)))

             (set? expr)
             (and (= (count expr) (count template))
                  (every? true? (map p-equal-loop
                                     (sort expr)
                                     (sort template))))

             (map? expr)
             (and (= (count expr) (count template))
                  (every? true? (map p-equal-loop
                                     (sort (keys expr))
                                     (sort (keys template))))
                  (every? true? (map p-equal-loop
                                     (map #(get expr %) (keys expr))
                                     (map #(get template %) (keys expr)))))

             :else (= expr template))))

(defn p-equal
  [template]
  (Matcher. (fn [zloc]
              (let [expr (z/sexpr zloc)]
                (p-equal-loop expr template)))))

(defn p-meta [template]
  (Matcher. (fn [zloc]
              (-> zloc z/sexpr meta (= template)))))

(defn check-with-meta [f]
  (fn [zloc]
    (if (-> zloc z/tag (= :meta))
      (f (-> zloc z/down z/right))
      (f zloc))))

(defn p-type [template]
  (Matcher. (check-with-meta
             (fn [zloc]
               (-> zloc z/tag (= template))))))

(defn p-form [template]
  (Matcher. (fn [zloc]
              (and (-> zloc z/tag (= :list))
                   (-> zloc z/down z/value (= template))))))

(defn p-pattern [template]
  (Matcher. (fn [zloc]
              (let [mf (pattern-fn template)]
                (-> zloc z/sexpr mf)))))

(defn p-code
  [template]
  (Matcher. (fn [zloc]
              (cond (regex? template)
                    (if (->> zloc z/->string (re-find template))
                      true false)))))

(defn p-and [& matchers]
  (Matcher. (fn [zloc]
              (->> (map (fn [m] (m zloc)) matchers)
                   (every? true?)))))

(defn p-or [& matchers]
  (Matcher. (fn [zloc]
              (->> (map (fn [m] (m zloc)) matchers)
                   (some true?)))))

(declare p-ancestor p-contains p-parent p-child p-first p-sibling p-right p-left)

(defn compile-matcher [template]
  (cond (-> template meta :-) (p-is template)
        (-> template meta :%) (compile-matcher {:pattern template})
        (symbol? template)    (compile-matcher {:form template})
        (fn? template)        (compile-matcher {:is template})
        (list? template)      (compile-matcher {:pattern template})
        (regex? template)     (compile-matcher {:code template})
        (vector? template)    (apply p-and (map compile-matcher template))
        (set? template)       (apply p-or (map compile-matcher template))
        (hash-map? template)
        (apply p-and
               (map (fn [[k v]]
                      (condp = k
                        :fn       (p-fn v)
                        :is       (p-is v)
                        :equal    (p-equal v)
                        :type     (p-type v)
                        :meta     (p-type v)
                        :form     (p-form v)
                        :pattern  (p-pattern v)
                        :code     (p-code v)
                        :parent   (p-parent v)
                        :first    (p-first v)
                        :child    (p-child v)
                        :ancestor (p-ancestor v)
                        :contains (p-contains v)
                        :sibling  (p-sibling v)
                        :right    (p-right v)
                        :left     (p-left v)))
                    template))))

(defn p-parent [template]
  (let [template (if (symbol? template) {:form template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
               (if-let [parent (z/up zloc)]
                 (and (not= (z/sexpr zloc)
                            (z/sexpr parent))
                      (m-fn parent)))))))

(defn p-first [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
               (if-let [child (z/down zloc)]
               (m-fn child))))))

(defn p-child [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (if-let [child (z/down zloc)]
                  (->> child
                       (iterate z/right)
                       (take-while identity)
                       (map m-fn)
                       (some identity)
                       nil? not)
                  false)))))

(defn tree-search
  ([zloc m-fn dir1 dir2]
     (if zloc
       (cond (nil? zloc) nil
             (m-fn zloc) true
             :else
             (or (tree-search (dir1 zloc) m-fn dir1 dir2)
                 (tree-search (dir2 zloc) m-fn dir1 dir2))))))

(defn p-contains [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (-> zloc (z/down) (tree-search m-fn z/right z/down))))))

(defn p-ancestor [template]
  (let [template (if (symbol? template) {:form template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (-> zloc (z/up) (tree-search m-fn z/up (fn [_])))))))

(defn p-left [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (if-let [left (-> zloc z/left)]
                  (m-fn left))))))

(defn p-right [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (if-let [right (-> zloc z/right)]
                  (m-fn right))))))

(defn p-sibling [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (or (-> zloc z/right (tree-search m-fn z/right (fn [_])))
                    (-> zloc z/left  (tree-search m-fn z/left (fn [_])))
                    false)))))
