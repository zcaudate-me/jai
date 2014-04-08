(ns juy.core
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.printer :as p]
            [hara.common.checks :refer [hash-map?]]
            [juy.match :refer [match-template]]))

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

(defn prewalk
  [zloc m f]
  (let [nloc  (if (m zloc)
                (f zloc)
                zloc)
        nloc  (if-let [zdown (z/down nloc)]
                (z/up (prewalk zdown m f))
                nloc)
        nloc  (if-let [zright (z/right nloc)]
                (z/left (prewalk zright m f))
                nloc)]
    nloc))

(defn postwalk
  [zloc m f]
  (let [nloc  (if-let [zdown (z/down zloc)]
                (z/up (postwalk zdown m f))
                zloc)
        nloc  (if-let [zright (z/right nloc)]
                (z/left (postwalk zright m f))
                nloc)
        nloc  (if (m nloc)
                (f nloc)
                nloc)]
    nloc))

(defn matchwalk
  [zloc [m & more :as matchers] f]
  (let [nloc (if (m zloc)
               (cond (empty? more)
                     (f zloc)

                     (z/down zloc)
                     (z/up (matchwalk (z/down zloc) more f))

                     :else
                     zloc)
               zloc)
        nloc  (if-let [zdown (z/down zloc)]
                (z/up (matchwalk zdown matchers f))
                zloc)
        nloc  (if-let [zright (z/right nloc)]
                (z/left (matchwalk zright matchers f))
                nloc)]
    nloc))


(comment
  {:order (< 2)
   :right {:right string?}}

  (def sample
    (z/of-string
     "
(do 1 2 3)
(defn      id        [x] x)
(defn      id        [x] x)
(defn lister [x]
   (list 1 2 3 4 x))"
     ))


  (matchwalk sample
             [(compile-matcher  'defn)
              (compile-matcher  {:is number?})]
             (fn [node]
               (println (z/sexpr node))
               node
               ))

  (prewalk sample
           (compile-matcher {:is number?})
           (fn [node]
             (println (z/sexpr node))
             node
             ))


  (prewalk sample
           (compile-matcher 'defn)
           (fn [node]
             (println (z/sexpr node))
             node
             ))

  (postwalk sample
            (compile-matcher 'defn)
            (fn [node]
              (println (z/sexpr node))
              node
              ))

  (defn postwalk
    [zloc pred? f level direction]
    (condp = direction
      :down (if-let [zdown (z/down)]
              (postwalk zdown pred? f (inc level) :down))))

  (defn postwalk
    ([zloc f]
       ())
    ([zloc p f]))


  (comment

    (matchwalk sample
               [(compile-matcher 'defn)
                (compile-matcher {:right number?})]
               (fn [node]
                 ;;(z/replace node "hello")
                 (println (z/sexpr node))
                 node
                 ))

    (def a (let [atm (atom [])]
             (-> (postwalk sample
                           (compile-matcher {:right number?})
                           (fn [node]
                             (z/replace node "hello")
                             ;;(println (z/sexpr node))
                             ;;node
                             ))
                 (z/sexpr))
             ))

    (def a (let [atm (atom [])]
             (-> (postwalk sample
                           (compile-matcher 'defn)
                           (fn [node]
                             (postwalk node
                                       (compile-matcher {:right number?})
                                       (fn [node]
                                         (z/replace node "hello")))))

                 (z/->root-string))))

    (println a)

    (let [atm (atom [])]
      (-> (z/prewalk sample
                     (?-right (?-right number?))
                     (fn [x]
                       (swap! atm conj x)
                       x)))
      (map (juxt (comp meta z/node) z/sexpr) @atm))

    ([{:row 4, :col 5} list] [{:row 4, :col 10} 1] [{:row 4, :col 12} 2])

    (let [atm (atom [])]
      (-> (z/prewalk sample
                     (?-left (?-left number?))
                     (fn [x]
                       (swap! atm conj x)
                       x)))
      (map (juxt (comp meta z/node) z/sexpr) @atm))

    (>pst)

    => ([{:row 4, :col 5} list] [{:row 4, :col 10} 1] [{:row 4, :col 12} 2] [{:row 4, :col 14} 3])


    (let [atm (atom [])]
      (-> (z/prewalk sample
                     (?-left number?)
                     (fn [x]
                       (swap! atm conj x)
                       x)))
      (map (juxt (comp meta z/node) z/sexpr) @atm))

    => ([{:row 4, :col 12} 2] [{:row 4, :col 14} 3] [{:row 4, :col 16} 4]))




















  (comment
    (let [atm (atom [])]
      (-> (z/prewalk sample
                     (?-is '(defn _ & _))
                     (fn [x]
                       (swap! atm conj x)
                       x)))
      (map (juxt (comp meta z/node) z/sexpr) @atm))

    (let [atm (atom [])]
      (-> (z/prewalk sample
                     (?-right string?)
                     (fn [x]
                       (swap! atm conj x)
                       x)))
      (map (juxt (comp meta z/node) z/sexpr) @atm))

    (let [atm (atom [])]
      (-> (z/prewalk sample
                     (?-symbol 'defn)
                     (fn [x]
                       (swap! atm conj x)
                       x)))
      (map (juxt (comp meta z/node) z/->string) @atm))

    (comment



      (((quote defn) (quote id) (((quote if) true & _) :seq) & _) :seq)


      (list 'let ['x ]
            (clojure.core.match/clj-form
             '[x]
             '[[(((quote defn) (quote id) "oeuoeuo" [1 2 3 4] & _) :seq)] true :else false]))


      '(defn id & _)
      => [[(((quote defn) (quote id) & _) :seq)] true :else false]


      (z/sexpr (z/of-string (with-out-str (prn '(defn id [x] x)))))


      (def data (z/of-file "../hara/src/hara/common/error.clj"))

      (z/find-value data z/right 'defn)

      (def prj-map (z/find-value data z/next 'defproject))


      (-> sample z/tag)
      (-> sample z/next z/value)
      (z/next)
      (z/next)
      (z/node)
      meta

      ($ sample [{:is id
                  :contains "hello"}])

      ($ sample [{:is   id}])


      (defn ?left-of [sym])


      (defn ?right-of [sym])

      (-> (z/prewalk sample
                     (fn [x] (= 'id (z/sexpr x)))
                     (fn [x] (z/replace x 'hello)))
          (z/print-root))

      (def pair
        (let [atm (atom [])]
          (-> (z/prewalk sample
                         (symbol-pred 'defn)
                         (fn [x]
                           (swap! atm conj x)
                           x)))
          @atm))

      (map (juxt (comp meta z/node) z/sexpr) pair)


      (def pair-is
        (let [atm (atom [])]
          (-> (z/prewalk sample
                         (is-pred 'defn)
                         (fn [x]
                           (swap! atm conj x)
                           x)))
          @atm))

      (map (juxt (comp meta z/node) z/sexpr) pair-is)

      (def data (z/of-string "(1 (2 3))"))

      (-> data
          (z/next)
          (z/replace 2)
          (z/insert-left 1)
          (z/insert-left  [:whitespace "        "])
          (z/up)
          (z/sexpr))

      (z/sexpr sample))

    (z/value sample)

    (z/node (-> sample z/next z/next ))

    (z/node (z/right* sample))

    (z/sexpr (z/right* (z/right sample)))

    (defn $ [form & ])

    (def data '((defn id  [x] x)
                (defn id2 [x] x)
                (defn listr [x]
                  (list 1 2 3 4 x))))
    (comment
      ($ data [(defn is ^:data [] ...)
               {:contains :list}
               {:value    :vector}
               hello
               [{is :whitespace}]])

      (filter

       {:type      :vector
        :value     [1 _ 3]
        :has-child hello
        :next
        :prev
        :sibling   {:type :vector}}



       )


      (-> ($ ' [(-> )])
          (fn [x] 4))
      )
    )
)
