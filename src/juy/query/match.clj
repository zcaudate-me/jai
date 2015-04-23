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
  "takes a predicate function to check the state of the zipper
  ((p-fn (fn [x]
           (-> (z/node x) (.tag) (= :token))))
   (z/of-string \"defn\"))
  => true"
  {:added "0.1"}
  [template]
  (Matcher. (fn [zloc]
              (template zloc))))

(defn p-is
  "checks if node is equivalent, does not meta into account
  ((p-is 'defn) (z/of-string \"defn\"))
  => true

  ((p-is '^{:a 1} defn) (z/of-string \"defn\"))
  => true
  
  ((p-is 'defn) (z/of-string \"is\"))
  => false

  ((p-is '(defn & _)) (z/of-string \"(defn x [])\"))
  => false"
  {:added "0.1"}
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
  "checks if the node is equivalent, takes meta into account
  ((p-equal '^{:a 1} defn) (z/of-string \"defn\"))
  => false

  ((p-equal '^{:a 1} defn) (z/of-string \"^{:a 1} defn\"))
  => true

  ((p-equal '^{:a 1} defn) (z/of-string \"^{:a 2} defn\"))
  => false"
  {:added "0.1"}
  [template]
  (Matcher. (fn [zloc]
              (let [expr (z/sexpr zloc)]
                (p-equal-loop expr template)))))

(defn p-meta
  "checks if meta is the same
  ((p-meta {:a 1}) (z/of-string \"^{:a 1} defn\"))
  => true
  
  ((p-meta {:a 1}) (z/of-string \"^{:a 2} defn\"))
  => false"
  {:added "0.1"}
  [template]
  (Matcher. (fn [zloc]
              (-> zloc z/sexpr meta (= template)))))

(defn check-with-meta [f]
  (fn [zloc]
    (if (-> zloc z/tag (= :meta))
      (f (-> zloc z/down z/right))
      (f zloc))))

(defn p-type
  "check on the type of element
  ((p-type :token) (z/of-string \"defn\"))
  => true
  
  ((p-type :token) (z/of-string \"^{:a 1} defn\"))
  => true"
  {:added "0.1"}
  [template]
  (Matcher. (check-with-meta
             (fn [zloc]
               (-> zloc z/tag (= template))))))

(defn p-form
  "checks if it is a form with the symbol as the first element
  ((p-form 'defn) (z/of-string \"(defn x [])\"))
  => true
  ((p-form 'let) (z/of-string \"(let [])\"))
  => true"
  {:added "0.1"}
  [template]
  (Matcher. (fn [zloc]
              (and (-> zloc z/tag (= :list))
                   (-> zloc z/down z/value (= template))))))

(defn p-pattern
  "checks if the form matches a particular pattern
  ((p-pattern '(defn ^:% symbol? & _)) (z/of-string \"(defn ^{:a 1} x [])\"))
  => true

  ((p-pattern '(defn ^:% symbol? ^:%? string? [])) (z/of-string \"(defn ^{:a 1} x [])\"))
  => true
  "
  {:added "0.1"}
  [template]
  (Matcher. (fn [zloc]
              (let [mf (pattern-fn template)]
                (-> zloc z/sexpr mf)))))

(defn p-code
  "checks if the form matches a string in the form of a regex expression
  ((p-code #\"defn\") (z/of-string \"(defn ^{:a 1} x [])\"))
  => true"
  {:added "0.1"}
  [template]
  (Matcher. (fn [zloc]
              (cond (regex? template)
                    (if (->> zloc z/->string (re-find template))
                      true false)))))

(defn p-and
  "takes multiple predicates and ensures that all are correct
  ((p-and (p-code #\"defn\")
          (p-type :token)) (z/of-string \"(defn ^{:a 1} x [])\"))
  => false

  ((p-and (p-code #\"defn\")
          (p-type :list)) (z/of-string \"(defn ^{:a 1} x [])\"))
  => true"
  {:added "0.1"}
  [& matchers]
  (Matcher. (fn [zloc]
              (->> (map (fn [m] (m zloc)) matchers)
                   (every? true?)))))

(defn p-or
  "takes multiple predicates and ensures that at least one is correct
  ((p-or (p-code #\"defn\")
          (p-type :token)) (z/of-string \"(defn ^{:a 1} x [])\"))
  => true

  ((p-or (p-code #\"defn\")
          (p-type :list)) (z/of-string \"(defn ^{:a 1} x [])\"))
  => true"
  {:added "0.1"}
  [& matchers]
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

(defn p-parent
  "checks that the parent of the element contains a certain characteristic
  ((p-parent 'defn) (-> (z/of-string \"(defn x [])\") z/next z/next))
  => true

  ((p-parent {:parent 'if}) (-> (z/of-string \"(if (= x y))\") z/down z/next z/next))
  => true
  
  ((p-parent {:parent 'if}) (-> (z/of-string \"(if (= x y))\") z/down))
  => false"
  {:added "0.1"}
  [template]
  (let [template (if (symbol? template) {:form template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
               (if-let [parent (z/up zloc)]
                 (and (not= (z/sexpr zloc)
                            (z/sexpr parent))
                      (m-fn parent)))))))

(defn p-first
  "checks that the first element of the container has a certain characteristic
  ((p-first 'defn) (-> (z/of-string \"(defn x [])\")))
  => true
  
  ((p-first 'x) (-> (z/of-string \"[x y z]\")))
  => true

  ((p-first 'x) (-> (z/of-string \"[y z]\")))
  => false"
  {:added "0.1"}
  [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
               (if-let [child (z/down zloc)]
               (m-fn child))))))

(defn p-child
  "checks that there is a child of a container that has a certain characteristic
  ((p-child {:form '=}) (z/of-string \"(if (= x y))\"))
  => true

  ((p-child '=) (z/of-string \"(if (= x y))\"))
  => false"
  {:added "0.1"}
  [template]
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

(defn p-contains
  "checks that any element (deeply nested also) of the container matches
  ((p-contains '=) (z/of-string \"(if (= x y))\"))
  => true

  ((p-contains 'x) (z/of-string \"(if (= x y))\"))
  => true"
  {:added "0.1"}
  [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (-> zloc (z/down) (tree-search m-fn z/right z/down))))))

(defn p-ancestor
  "checks that any parent container matches
  ((p-ancestor {:form 'if}) (-> (z/of-string \"(if (= x y))\") z/down z/next z/next))
  => true
  ((p-ancestor 'if) (-> (z/of-string \"(if (= x y))\") z/down z/next z/next))
  => true"
  {:added "0.1"}
  [template]
  (let [template (if (symbol? template) {:form template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (-> zloc (z/up) (tree-search m-fn z/up (fn [_])))))))

(defn p-left
  "checks that the element on the left has a certain characteristic
  ((p-left '=) (-> (z/of-string \"(if (= x y))\") z/down z/next z/next z/next))
  => true
  
  ((p-left 'if) (-> (z/of-string \"(if (= x y))\") z/down z/next))
  => true"
  {:added "0.1"}
  [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (if-let [left (-> zloc z/left)]
                  (m-fn left))))))

(defn p-right
  "checks that the element on the right has a certain characteristic
  ((p-right 'x) (-> (z/of-string \"(if (= x y))\") z/down z/next z/next))
  => true
  
  ((p-right {:form '=}) (-> (z/of-string \"(if (= x y))\") z/down))
  => true"
  {:added "0.1"}
  [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (if-let [right (-> zloc z/right)]
                  (m-fn right))))))

(defn p-sibling
  "checks that any element on the same level has a certain characteristic
  ((p-sibling '=) (-> (z/of-string \"(if (= x y))\") z/down z/next z/next))
  => false
  
  ((p-sibling 'x) (-> (z/of-string \"(if (= x y))\") z/down z/next z/next))
  => true"
  {:added "0.1"}
  [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (or (-> zloc z/right (tree-search m-fn z/right (fn [_])))
                    (-> zloc z/left  (tree-search m-fn z/left (fn [_])))
                    false)))))
