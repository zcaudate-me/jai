(ns jai.query-test
  (:use midje.sweet)
  (:require [jai.query :refer :all]
            [rewrite-clj.zip :as z]))

^{:refer jai.query/cursor-info :added "0.2"}
(fact "figure out where the cursor is in the path"

  (cursor-info '[defn if])
  => [nil :cursor]

  (cursor-info '[| defn if])
  => [0 :cursor]

  (cursor-info '[defn (if & _)])
  => [1 :form '(if & _)]

  ^:hidden
  
  (cursor-info '[defn | if])
  => [1 :cursor]
  
  (cursor-info '[defn (if | & _)])
  => [1 :form '(if | & _)])

^{:refer jai.query/expand-all-metas :added "0.2"}
(fact "expand-all-metas"
  (map meta
       (expand-all-metas '(^:+? defn ^:-% hello)))
  => [{:+ true, :? true} {:- true, :% true}]

  
  )
