(ns jai.common-test
  (:use midje.sweet)
  (:require [jai.common :refer :all]))

(fact "seperates out the meta into individual flags"
  (meta (expand-meta ^:? ()))
  => {:? true}
  (meta (expand-meta ^:+%? ()))
  => {:+ true, :? true, :% true})

(fact "removes items from a form matching the predicate"
  (remove-items #(= 1 %) '(1 2 3 4))
  => '(2 3 4)

  (remove-items #(= 1 %) '(1 (1 (1 (1)))))
  => '(((()))))


(fact "returns the index of the first occurence"
  (find-index #(= 2 %) '(1 2 3 4))
  => 1)
