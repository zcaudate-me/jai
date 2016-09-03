(ns jai.common-test
  (:use [hara.test :exclude [any]])
  (:require [jai.common :refer :all]))

^{:refer jai.common/any :added "0.2"}
(fact "returns true for any value"
  (any nil) => true
  (any '_) => true)

^{:refer jai.common/none :added "0.2"}
(fact "returns false for any value"
  (none nil) => false	
  (none '_) => false)

^{:refer jai.common/expand-meta :added "0.2"}
(fact "separates out the meta into individual flags"
  (meta (expand-meta ^:? ()))
  => {:? true}
  (meta (expand-meta ^:+%? ()))
  => {:+ true, :? true, :% true})

^{:refer jai.common/cursor? :added "0.2"}
(fact "checks if element is `|`"
  (cursor? '|) => true
  (cursor? '_) => false)

^{:refer jai.common/insertion? :added "0.2"}
(fact "checks if element has an insert meta"
  (insertion? '^:+ a) => true
  (insertion? 'a) => false)

^{:refer jai.common/deletion? :added "0.2"}
(fact "checks if element has a delete meta"
  (deletion? '^:- a) => true
  (deletion? 'a) => false)

^{:refer jai.common/remove-items :added "0.2"}
(fact "removes items from a form matching the predicate"
  (remove-items #(= 1 %) '(1 2 3 4))
  => '(2 3 4)

  (remove-items #(= 1 %) '(1 (1 (1 (1)))))
  => '(((()))))

^{:refer jai.common/prepare-deletion :added "0.2"}
(fact "removes extraneous symbols for deletion walk"
  (prepare-deletion '(+ a 2))
  => '(+ a 2)

  (prepare-deletion '(+ ^:+ a | 2))
  => '(+ 2))

^{:refer jai.common/prepare-insertion :added "0.2"}
(fact "removes extraneous symbols for deletion walk"
  (prepare-insertion '(+ a 2))
  => '(+ a 2)

  (prepare-insertion '(+ ^:+ a | ^:- b 2))
  => '(+ a 2))

^{:refer jai.common/find-index :added "0.2"}
(fact "returns the index of the first occurrence"
  (find-index #(= 2 %) '(1 2 3 4))
  => 1)

^{:refer jai.common/finto :added "0.2"}
(fact "into but the right way for lists"
  (finto () '(1 2 3))
  => '(1 2 3))
