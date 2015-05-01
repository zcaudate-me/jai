(ns jai.query.pattern-test
  (:use midje.sweet)
  (:require [jai.query.pattern :refer :all]))

^{:refer jai.query.pattern/transform-pattern :added "0.1"}
(fact "turns a jai pattern into a core.match pattern"
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
  => {symbol? 1})

^{:refer jai.query.pattern/pattern-fn-regex :added "0.1"}
(fact "pattern matching for regex"
  ((pattern-fn #"w.*") "world")
  => true

  ((pattern-fn #"h.*") "world")
  => false

  ((pattern-fn #"h") #"h")
  => true)

^{:refer jai.query.pattern/pattern-fn-pred :added "0.1"}
(fact "pattern matching for predicates" 
  ((pattern-fn string?) "hello")
  => true

  ((pattern-fn number?) "world")
  => false)

^{:refer jai.query.pattern/pattern-fn-coll :added "0.1"}
(fact "pattern matching for collections"
  
  ((pattern-fn [1 2 3]) [1 2 3])
  => true

  ((pattern-fn [1 2 3]) '(1 2 3))
  => false

  ((pattern-fn {:a string?}) {:a "Hello"})
  => true)

^{:refer jai.query.pattern/pattern-fn-% :added "0.1"}
(fact "pattern matching for :% modifier"
  ((pattern-fn 'symbol?) 'symbol?)
  => true

  ((pattern-fn 'symbol?) 'a)
  => false

  ((pattern-fn  '^:% symbol?) 'a)
  => true

  ((pattern-fn '(defn y ^:% vector?)) '(defn y []))
  => true
  
  ((pattern-fn '(defn ^:% symbol? ^:% empty?)) '(defn y []))
  => true)

^{:refer jai.query.pattern/pattern-fn-args :added "0.1"}
(fact "pattern amtching for '&' and '_' symbols"
  ((pattern-fn '(defn _ '[& _])) '(defn hello [& _]))
  => false

  ((pattern-fn '(defn _ '[& _])) '(defn hello '[& _]))
  => true

  ((pattern-fn '(defn _ '[& _])) '(defn hello '[a b c]))
  => true ;; should really be false
  
  ((pattern-fn '(defn _ ['& '_])) '(defn hello [& _]))
  => true
  
  ((pattern-fn '(defn _ ['& '_])) '(defn hello [& args]))
  => false

  ((pattern-fn '(defn _ ['& _])) '(defn hello [& args]))
  => true

  ((pattern-fn '(defn _ ['& _])) '(defn hello [x y z]))
  => false

  ((pattern-fn '(defn _ [& _])) '(defn hello [x y z]))
  => true)

^{:refer jai.query.pattern/pattern-fn-match :added "0.1"}
(fact "pattern matching for core.match types"
  
  ((pattern-fn '(defn & _)) '(defn x []))
  => true
  
  ((pattern-fn '(defn y)) '(defn x []))
  => false

  ((pattern-fn ''&) '&)
  => true
  
  ((pattern-fn ''&) '1)
  => false)

^{:refer jai.query.pattern/pattern-fn-optional :added "0.1"}
(fact "pattern matching for optional types"
  (map (pattern-fn '(defn ^:% symbol? ^:%? string? ^:%? map? ^:% vector? & _))
       '[(defn x [])
         (defn x {} [])
         (defn x "" [])
         (defn x "" {} [])
         (defn x "" {} [] (inc 1))
         (defn x [] (inc 1))])
  => [true true true true true true])

(comment
  (pattern-form 'a '1)
    => (try (clojure.core/cond
              (clojure.core/= a 1) true
              :else (throw clojure.core.match/backtrack))
            (catch Exception e__13377__auto__
              (if (clojure.core/identical? e__13377__auto__
                                           clojure.core.match/backtrack)
                (do false)
                (throw e__13377__auto__))))
    
  (pattern-form 'a '[x y z])
  => 
  (try (cond (and (vector? a)
                  (== (count a) 3))
             (try (let [a_0__25177 (nth a 0)]
                    (cond
                      (= a_0__25177 (quote x))
                      (try
                        (let [a_1__25178 (nth a 1)]
                          (cond (= a_1__25178 (quote y))
                                (try (let [a_2__25179 (nth a 2)]
                                       (cond (= a_2__25179 (quote z)) true :else
                                             (throw clojure.core.match/backtrack)))
                                     (catch Exception e__13377__auto__
                                       (if (identical? e__13377__auto__ clojure.core.match/backtrack)
                                         (do (throw clojure.core.match/backtrack))
                                         (throw e__13377__auto__))))
                                :else (throw clojure.core.match/backtrack)))
                         (catch Exception e__13377__auto__
                           (if (identical? e__13377__auto__ clojure.core.match/backtrack)
                             (do (throw clojure.core.match/backtrack))
                             (throw e__13377__auto__))))
                      :else (throw clojure.core.match/backtrack)))
                  (catch Exception e__13377__auto__
                    (if (identical? e__13377__auto__ clojure.core.match/backtrack)
                      (do (throw clojure.core.match/backtrack))
                      (throw e__13377__auto__))))
             :else (throw clojure.core.match/backtrack))
       (catch Exception e__13377__auto__
         (if (identical? e__13377__auto__ clojure.core.match/backtrack)
           (do false)
           (throw e__13377__auto__)))))

