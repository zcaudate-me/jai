(ns juy.query.path-test
  (:use midje.sweet)
  (:require [juy.query.path :refer :all]
            [juy.query.match :as match]
            [juy.query.walk :as walk]
            [rewrite-clj.zip :as z]))

^{:refer juy.query.path/find-cursor-index :added "0.1"}
(fact "finds the :| keyword in a vector"
  (find-cursor-index [])
  => -1

  (find-cursor-index '[try])
  => -1

  (find-cursor-index '[try :|])
  => 1)

^{:refer juy.query.path/group-items :added "0.1"}
(fact "groups "
  (group-items '[try if])
  => '[[:> try] [:> if]]
  
  (group-items '[try :* if])
  => '[[:* try] [:> if]])

^{:refer juy.query.path/compile-path :added "0.1"}
(fact "compiles the path for search"
  (compile-path '[try])
  => '{:form try}

  (compile-path '[try :|])
  => '{:parent {:form try}}

  (compile-path '[try :* hello])
  => '{:contains {:form hello}, :form try}

  (compile-path '[try :| :* hello])
  => '{:contains {:form hello}, :parent {:form try}}

  (compile-path '[try :* :| hello])
  => '{:form hello, :ancestor {:form try}})
