(ns juy.core
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.printer :as p]
            [juy.match :refer [match-template]]))

(defn ?-symbol [form]
  (fn [node]
    (and (-> node z/tag (= :list))
         (-> node z/down z/value (= form)) )))

(defn query [form template]
  (cond (fn? template) (template form)
        (coll? template) (match-template form template)
        :else (= template form)))

(defn ?-is [template]
  (fn [node]
    (-> node z/sexpr (query template))))

(defn ?-contains [template]
  (fn [node]
    (if (-> node z/tag (= :list))
      (->> (z/down node)
           (iterate z/right)
           (take-while identity)
           (map z/sexpr)
           (map #(query % template))
           (some identity)))))

(defn ?-left [template]
  (fn [node]
    (-> node z/left z/sexpr (query template))))

(defn ?-right [template]
  (fn [node]
    (-> node z/right z/sexpr (query template))))


(def sample
(z/of-string
"
(defn      id        [x] x)
(defn lister [x]
   (list 1 2 3 4 x))"
))

(comment


  (let [atm (atom [])]
    (-> (z/prewalk sample
                   (?-right number?)
                   (fn [x]
                     (swap! atm conj x)
                     x)))
    (map (juxt (comp meta z/node) z/sexpr) @atm))

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
