(ns jai.traverse-test
  (:use midje.sweet)
  (:require [rewrite-clj.zip :as source]
            [clojure.zip :as zip]
            [jai.traverse :refer :all]))

(fact
  (traverse (source/of-string "^:a (+ () 2 3)")
            '(+ () 2 3))
  
  (traverse (source/of-string "^:a (hello)")
            '(hello))
  
  (traverse (source/of-string "^:a (hello)")
            '(^:- hello))
  
  (traverse (source/of-string "(hello)")
            '(^:- hello))

  (traverse (source/of-string "((hello))")
            '((^:- hello)))


  ;; Insertions
  (traverse (source/of-string "()")
            '(^:+ hello))

  (traverse (source/of-string "(())")
            '((^:+ hello)))

  
  (traverse (source/of-string "(defn hello)")
            '(defn ^{:? true :% true :- true} symbol? ^:+ []))
  )

(fact
  (traverse (source/of-string "(defn hello)")
            '(defn ^{:? true :% true :- true} symbol? ^:+ []))
  

  (traverse (source/of-string "(defn hello)")
            '(defn ^{:? true :% true :- true} symbol? ^:+ []))

  (traverse (source/of-string "(defn hello)")
            '(defn ^{:? true :% true :- true} symbol? | ^:+ []))

  (source/sexpr (traverse (source/of-string "(defn hello [] (+ 1 1))")
                          '(defn _ _ (+ | 1 & _))))
  => nil
  )

(set! *print-meta* false)
