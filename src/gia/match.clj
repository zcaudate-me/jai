(ns gia.query.match
  (:require [rewrite-clj.zip :as z]
            [hara.common.checks :refer [hash-map? regex?]]
            [gia.query.pattern :refer [pattern-fn]]))

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

(declare p-parent p-child p-first p-last p-nth p-nth-left p-nth-right
         p-nth-ancestor p-nth-contains p-ancestor p-contains
         p-sibling p-left p-right p-right-of p-left-of p-right-most p-left-most)

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
                      (case k
                        :fn           (p-fn v)
                        :is           (p-is v)
                        :or           (apply p-or (map compile-matcher template))
                        :equal        (p-equal v)
                        :type         (p-type v)
                        :meta         (p-type v)
                        :form         (p-form v)
                        :pattern      (p-pattern v)
                        :code         (p-code v)
                        :parent       (p-parent v)
                        :child        (p-child v)
                        :first        (p-first v)
                        :last         (p-last v)
                        :nth          (p-nth v)
                        :nth-left     (p-nth-left v)
                        :nth-right    (p-nth-right v)
                        :nth-ancestor (p-nth-ancestor v)
                        :nth-contains (p-nth-contains v)
                        :ancestor     (p-ancestor v)
                        :contains     (p-contains v)
                        :sibling      (p-sibling v)
                        :left         (p-left v)
                        :right        (p-right v)
                        :left-of      (p-left-of v)
                        :right-of     (p-right-of v)
                        :left-most    (p-left-most v)
                        :right-most   (p-right-most v)))
                    template))
        :else (compile-matcher {:is template})))

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

(defn p-last
  "checks that the last element of the container has a certain characteristic
  ((p-last 1) (-> (z/of-string \"(defn [] 1)\")))
  => true
  
  ((p-last 'z) (-> (z/of-string \"[x y z]\")))
  => true

  ((p-last 'x) (-> (z/of-string \"[y z]\")))
  => false"
  {:added "0.1"}
  [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
               (if-let [child (-> zloc z/down z/rightmost)]
                 (m-fn child))))))

(defn p-nth
  "checks that the last element of the container has a certain characteristic
  ((p-nth [0 'defn]) (-> (z/of-string \"(defn [] 1)\")))
  => true
  
  ((p-nth [2 'z]) (-> (z/of-string \"[x y z]\")))
  => true

  ((p-nth [2 'x]) (-> (z/of-string \"[y z]\")))
  => false"
  {:added "0.1"}
  [[num template]]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (if-let [child (->> zloc z/down)]
                  (let [child (if (zero? num)
                                child
                                (-> (iterate z/next child) (nth num)))]
                    (m-fn child)))))))

(defn- p-nth-move
  [num template directon]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (let [dir (if (zero? num)
                            zloc
                            (-> (iterate directon zloc) (nth num)))]
                  (m-fn dir))))))

(defn p-nth-left
  "checks that the last element of the container has a certain characteristic
  ((p-nth-left [0 'defn]) (-> (z/of-string \"(defn [] 1)\") z/down))
  => true

  ((p-nth-left [1 ^:% vector?]) (-> (z/of-string \"(defn [] 1)\") z/down z/rightmost))
  => true"
  {:added "0.1"}
  [[num template]]
  (p-nth-move num template z/left))

(defn p-nth-right
  "checks that the last element of the container has a certain characteristic
  ((p-nth-right [0 'defn]) (-> (z/of-string \"(defn [] 1)\") z/down))
  => true
  
  ((p-nth-right [1 ^:% vector?]) (-> (z/of-string \"(defn [] 1)\") z/down))
  => true"
  {:added "0.1"}
  [[num template]]
  (p-nth-move num template z/right))

(defn p-nth-ancestor
  [[num template]]
  (p-nth-move num template z/up))

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

(defn tree-depth-search
  ([zloc m-fn level dir1 dir2]
   (if zloc
     (cond
       (< level 0) nil
       (and (= level 0) (m-fn zloc)) true
       :else
       (or (tree-depth-search (dir1 zloc) m-fn (dec level) dir1 dir2)
           (tree-depth-search (dir2 zloc) m-fn level dir1 dir2))))))

(defn p-nth-contains
  "checks that any element (deeply nested also) of the container matches
  ((p-contains '=) (z/of-string \"(if (= x y))\"))
  => true

  ((p-contains 'x) (z/of-string \"(if (= x y))\"))
  => true"
  {:added "0.1"}
  [[num template]]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (let [[dir num] (if (zero? num)
                                  [zloc num]
                                  [(z/down zloc) (dec num)])]
                  (tree-depth-search dir m-fn num z/down z/right))))))

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

(defn p-left-of
  "checks that any element on the left has a certain characteristic
  ((p-left-of '=) (-> (z/of-string \"(= x y)\") z/down z/next))
  => true
  
  ((p-left-of '=) (-> (z/of-string \"(= x y)\") z/down z/next z/next))
  => true"
  {:added "0.1"}
  [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (or (-> zloc z/left (tree-search m-fn z/left (fn [_])))
                    false)))))

(defn p-right-of
  "checks that any element on the right has a certain characteristic
  ((p-right-of 'x) (-> (z/of-string \"(= x y)\") z/down))
  => true
  
  ((p-right-of 'y) (-> (z/of-string \"(= x y)\") z/down))
  => true

  ((p-right-of 'z) (-> (z/of-string \"(= x y)\") z/down))
  => false"
  {:added "0.1"}
  [template]
  (let [template (if (symbol? template) {:is template} template)
        m-fn (compile-matcher template)]
    (Matcher. (fn [zloc]
                (or (-> zloc z/right (tree-search m-fn z/right (fn [_])))
                    false)))))

(defn p-left-most
  "checks that any element on the right has a certain characteristic
  ((p-left-most true) (-> (z/of-string \"(= x y)\") z/down))
  => true
  
  ((p-left-most true) (-> (z/of-string \"(= x y)\") z/down z/next))
  => false"
  {:added "0.1"}
  [bool]
  (Matcher. (fn [zloc] (= (-> zloc z/left nil?) bool))))

(defn p-right-most
  "checks that any element on the right has a certain characteristic
  ((p-right-most true) (-> (z/of-string \"(= x y)\") z/down z/next))
  => false
  
  ((p-right-most true) (-> (z/of-string \"(= x y)\") z/down z/next z/next))
  => true"
  {:added "0.1"}
  [bool]
  (Matcher. (fn [zloc] (= (-> zloc z/right nil?) bool))))
