(ns juy.query-test
  (:use midje.sweet)
  (:require [rewrite-clj.zip :as z]
            [juy.query :refer :all]
            [juy.query.path :as path]))

^{:refer juy.query/select :added "0.1"}
(fact "select code elements that follow the path pattern"
  (->> (select (z/of-file "src/juy/query/pattern/fn.clj")
               '[defmethod [^:# _ |]])
       
       (map z/sexpr))
  => '(match/emit-pattern match/to-source match/groupable?)

  (->> (select (z/of-file "src/juy/query/pattern/fn.clj")
               '[defmethod [^:# _ _ _ |]])
       
       (map z/sexpr))
  => '([pat] [pat ocr] [a b]))
