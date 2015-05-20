(ns jai.match.fn-test
  (:use midje.sweet)
  (:require  [jai.match [pattern :refer :all] fn]))

^{:refer jai.match.fn/pattern-fn :added "0.2"}
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
