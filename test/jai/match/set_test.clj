(ns jai.match.set-test
  (:use hara.test)
  (:require  [jai.match.pattern :refer :all]
             [clojure.core.match :as match]))

^{:refer jai.match.set/pattern-fn :added "0.2"}
(fact "make sure that sets are working properly"
  (transform-pattern #{1 2 3})
  => '(:or 1 3 2)

  ((pattern-fn #{1 2 3}) 3)
  => true
  
  ((pattern-fn #{1 2 3}) 4)
  => false
  
  ((pattern-fn #{'defn}) 'defn)
  => true
  
  ((pattern-fn #{#'symbol?}) 'defn)
  => true
  
  ((pattern-fn '#{^:% symbol? 1 2 3}) 'defn)
  => true
  
  ((pattern-fn '#{}) 'defn)
  => false

  ((pattern-fn #{1 2 3}) #{1 2 3})
  => false

  ((pattern-fn ^:& #{1 2 3}) #{1 2 3})
  => true)
