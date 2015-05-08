(ns jai.query.traverse-test
  (:use midje.sweet)
  (:require [rewrite-clj.zip :as source]
            [clojure.zip :as zip]
            [jai.query.traverse :refer :all]))

(fact
  (source/sexpr
   (traverse (source/of-string "^:a (+ () 2 3)")
             '(+ () 2 3)))
  => '(+ () 2 3)
  
  (source/sexpr
   (traverse (source/of-string "^:a (hello)")
             '(hello)))
  => '(hello)
  
  (source/sexpr
   (traverse (source/of-string "^:a (hello)")
             '(^:- hello)))
  => ()
  
  (source/sexpr
   (traverse (source/of-string "(hello)")
             '(^:- hello)))
  => ()
  
  (source/sexpr
   (traverse (source/of-string "((hello))")
             '((^:- hello))))
  => '(())

  ;; Insertions
  (source/sexpr
   (traverse (source/of-string "()")
             '(^:+ hello)))
  => '(hello)
  
  (source/sexpr
   (traverse (source/of-string "(())")
             '((^:+ hello))))
  => '((hello)))

(fact
  (source/sexpr
   (traverse (source/of-string "(defn hello)")
             '(defn ^{:? true :% true} symbol? ^:+ [])))
  => '(defn hello [])
  
  (source/sexpr
   (traverse (source/of-string "(defn hello)")
             '(defn ^{:? true :% true :- true} symbol? ^:+ [])))
  => '(defn [])
  
  (source/sexpr
   (traverse (source/of-string "(defn hello)")
             '(defn ^{:? true :% true :- true} symbol? | ^:+ [])))
  => []
  
  (source/sexpr
   (traverse (source/of-string "(defn hello [] (+ 1 1))")
             '(defn _ _ (+ | 1 & _))))
  => 1)
