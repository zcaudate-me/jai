(ns jai.query.traverse-test
  (:use midje.sweet)
  (:require [rewrite-clj.zip :as source]
            [clojure.zip :as zip]
            [jai.query.traverse :refer :all]))

^{:refer jai.traverse/traverse-basic :added "0.2"}
(defn source [pos]
  (-> pos :source source/sexpr))

^{:refer jai.traverse/traverse-basic :added "0.2"}
(fact
  (source
   (traverse (source/of-string "^:a (+ () 2 3)")
             '(+ () 2 3)))
  => '(+ () 2 3)
  
  (source
   (traverse (source/of-string "^:a (hello)")
             '(hello)))
  => '(hello)
  
  (source
   (traverse (source/of-string "^:a (hello)")
             '(^:- hello)))
  => ()
  
  (source
   (traverse (source/of-string "(hello)")
             '(^:- hello)))
  => ()
  
  (source
   (traverse (source/of-string "((hello))")
             '((^:- hello))))
  => '(())

  ;; Insertions
  (source
   (traverse (source/of-string "()")
             '(^:+ hello)))
  => '(hello)
  
  (source
   (traverse (source/of-string "(())")
             '((^:+ hello))))
  => '((hello)))

^{:refer jai.traverse/traverse-advance :added "0.2"}
(fact
  (source
   (traverse (source/of-string "(defn hello)")
             '(defn ^{:? true :% true} symbol? ^:+ [])))
  => '(defn hello [])
  
  (source
   (traverse (source/of-string "(defn hello)")
             '(defn ^{:? true :% true :- true} symbol? ^:+ [])))
  => '(defn [])
  
  (source
   (traverse (source/of-string "(defn hello)")
             '(defn ^{:? true :% true :- true} symbol? | ^:+ [])))
  => []

  (source
   (traverse (source/of-string "(defn hello \"world\" {:a 1} [])")
             '(defn ^:% symbol?
                 ^{:? true :% true :- true} string?
                 ^{:? true :% true :- true} map?
                 ^:% vector? & _)))
  => '(defn hello [])
  
  (source
   (traverse (source/of-string "(defn hello [] (+ 1 1))")
             '(defn _ _ (+ | 1 & _))))
  => 1

  (source
   (traverse (source/of-string "(defn hello [] (+ 1 1))")
             '(#{defn} | & _ )))
  => 'hello

  (source
   (traverse (source/of-string "(fact \"hello world\")")
             '(fact | & _ )))
  => "hello world")
