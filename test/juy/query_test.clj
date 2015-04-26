(ns juy.query-test
  (:use midje.sweet)
  (:require [rewrite-clj.zip :as z]
            [juy.query :refer :all]
            [juy.query.path :as path]))

^{:refer juy.query/select :added "0.1"}
(fact 
  (map z/sexpr
       (select (z/of-file "src/juy/query/pattern/fn.clj")
               '[defmethod :|]))

  
  => '([pat] [pat ocr] [a b]))
(comment

  (-> (z/of-file "src/juy/query/pattern/fn.clj")
      z/right
      z/sexpr)

  (map z/sexpr (select (z/of-file "src/juy/query/path.clj")
                       '[(defn & _)]))
  
(path/compile-path '[defmethod ^:% vector? []])
(path/compile-path '[defmethod :| {:is ^:% vector?}])
(path/compile-path '[defmethod :| ^:% {:is vector?}])
{:form vector?, :parent {:form defmethod}}
{:is vector?, :parent {:form defmethod}}


)
