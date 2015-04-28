(ns juy.query-test
  (:use midje.sweet)
  (:require [rewrite-clj.zip :as z]
            [juy.query :refer :all]
            [juy.query.path :as path]))

^{:refer juy.query/select :added "0.1"}
(fact "select code elements that follow the path pattern"
  (->> (select (z/of-file "src/juy/query/pattern/fn.clj")
               '[defmethod [^:# _ _ |]])
       
       (map z/sexpr))
  => '(match/emit-pattern match/to-source match/groupable?)

  (->> (select (z/of-file "src/juy/query/pattern/fn.clj")
               '[defmethod [^:# _ _ _ _ |]])
       
       (map z/sexpr))
  => '([pat] [pat ocr] [a b]))


(comment
  ($ "src/juy/query/pattern/fn.clj"
     [defmethod [_ | ^:$ _]])

  ($ "src/juy/query/pattern/fn.clj"
     [defmethod [^:# _ _]])
  => (match/emit-pattern match/to-source match/groupable?)

  ($ "src/juy/query/pattern/fn.clj"
     [(defmethod _ [clojure.lang.Fn clojure.lang.Fn] & _)
      [_ | :1 ^:$ _]])

  ($ "src/juy/query/pattern/fn.clj"
     [(defmethod _ [clojure.lang.Fn clojure.lang.Fn] & _)
      [^:# _ _]])
  (match/groupable?)

  (def fragment {:code "(defn hello [] (println \"hello world\"))"})

  
  (def fragment {:code "(defn hello [] (println \"hello\"))\n
                        (defn world [] (if true (prn \"world\")))"})

  ;; The simplest
  ($ fragment [defn])
  => '((defn hello [] (println "hello"))
       (defn world [] (if true (prn "world"))))

  ($ fragment [if])
  => '((if true (prn "world")))

  ($ fragment [defn if])
  => ((defn world [] (if true (prn "world"))))

  ($ fragment [defn :* prn])
  => ((defn world [] (if true (prn "world"))))

  ($ fragment [defn :2 prn])
  => '((defn world [] (if true (prn "world"))))

  ($ fragment [defn :3 prn])
  => '()
  
  
  
  ;; A pattern can also be used
  ($ fragment [(defn & _)])
  => '((defn hello [] (println "hello"))
       (defn world [] (if true (prn "world"))))
  
  ;; A pattern can also be a bit more specific
  ($ fragment [(defn hello & _)])
  => '((defn hello [] (println "hello")))

  ;; The pattern can also be nested
  ($ fragment [(defn world [] (if & _))])
  => '((defn world [] (if true (prn "world"))))

  ;; And contain modifiers
  ($ fragment [(defn world ^:% vector? ^:% list?)])
  => ((defn world [] (if true (prn "world"))))

  ;; 
  ($ fragment [(_ _ _ (if & _))])
  => '((defn world [] (if true (prn "world"))))
  
  ($ fragment [_ println])
  => '((defn hello [] (println "hello")))

  ($ fragment [_ if prn])
  => '((defn world [] (if true (prn "world"))))

  ($ fragment [defn :* prn])
  => '((defn world [] (if true (prn "world"))))
  
  ($ fragment [(defn & _)])
  => '((defn hello [] (println "hello"))
       (defn world [] (if true (prn "world"))))
  
  ($ fragment [(_ _ _ (if & _))])
  => '((defn world [] (if true (prn "world"))))
  
  ($ fragment [(defn _ [] & _)])
  => '((defn hello [] (println "hello"))
       (defn world [] (prn "world")))
  
  ($ fragment [(defn hello & _)])
  => '((defn hello [] (println "hello")))

  
  ($ fragment [defn prn])
  => '((defn world [] (prn "world")))

  ($ fragment [_ :* println])
  => '((defn hello [] (println "hello")))
  
  ($ fragment [defn | println])
  => '((println "hello"))

  ($ fragment [defn println [^:$ _]])
  => '("hello")

  ($ fragment [defn println [^:# _ _]])
  => '("hello")
  
  

  ($ {:code "(defn hello [] (println \"hello world\"))"}
     [_ :* println])
  => ((defn hello [] (println "hello world")))
  
  ($ {:code "(defn hello [] (println \"hello world\"))"}
     [defn | println])
  => '((println "hello world"))

  ($ {:code "(defn hello [] (println \"hello world\"))"}
     [defn | _])
  => '(defn hello [] (println "hello world"))

  
  ($ fragment [])
  '((defn hello [] (println "hello world"))
    defn
    hello
    []
    (println "hello world")
    println
    "hello world")
  
  )
