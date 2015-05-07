(ns jai.match.optional-test
  (:use midje.sweet)
  (:require [jai.match.optional :refer :all]))

(fact "generate a sequence of possible matches"
  (pattern-seq '(+ ^:? (1) ^:? (^:? + 2)))
  => '((+)
       (+ (1))
       (+ (2))
       (+ (1) (2))
       (+ (+ 2))
       (+ (1) (+ 2))))


