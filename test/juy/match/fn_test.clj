(ns gia.match.fn-test
  (:use midje.sweet)
  (:require  [gia.match [pattern :refer :all] fn]))

(fact "make sure that functions are working properly"
  ((pattern-fn vector?) [])
  => throws

  ((pattern-fn #'vector?) [])
  => true

  ((pattern-fn '^:% vector?) [])
  => true
  
  ((pattern-fn '^:% symbol?) [])
  => false

  ((pattern-fn '[^:% vector?]) [[]])
  => true)
