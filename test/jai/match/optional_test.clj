(ns jai.match.optional-test
  (:use hara.test)
  (:require [jai.match.optional :refer :all]))

^{:refer jai.match.optional/pattern-seq :added "0.2"}
(fact "generate a sequence of possible matches"
  (pattern-seq '(+ ^:? (1) ^:? (^:? + 2)))
  => '((+)
       (+ (1))
       (+ (2))
       (+ (1) (2))
       (+ (+ 2))
       (+ (1) (+ 2))))


