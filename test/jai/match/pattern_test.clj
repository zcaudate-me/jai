(ns jai.match.pattern-test
  (:use midje.sweet)
  (:require [jai.match.pattern :refer :all]))

(set! *print-meta* true)

(fact "pattern"
  ((pattern-matches ()) ())
  => '(())
  
  ((pattern-matches '(^:% symbol? ^:? (+ 1 _ ^:? _))) '(+ (+ 1 2 3)))
  => '((^{:% true} symbol? ^{:? 0} (+ 1 _ ^{:? 1} _))))


(set! *print-meta* false)

(comment)

