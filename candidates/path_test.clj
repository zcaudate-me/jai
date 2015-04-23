(ns juy.query.pattern.path-test
  (:use midje.sweet)
  (:require [juy.query.path :refer :all]
            [juy.query.match :as match]
            [juy.query.walk :as walk]
            [rewrite-clj.zip :as z]))

(def example
  (->
   "(try
      (if (nil? a)
       (hello there)
       (world))
     (catch Throwable t))"
   (z/of-string)))

(fact "compile-path examples"
  (compile-path '[try])
  => {:form try}

  (compile-path '[try :|])
  => {:parent {:form try}}

  (compile-path '[try :* hello])
  => {:contains {:form hello}, :form try}

  (compile-path '[try :| :* hello])
  => {:contains {:form hello}, :parent {:form try}}

  (compile-path '[try :* :| hello])
  => {:form hello, :ancestor {:form try}})



(->> '[:| try :* hello]
     (compile-path)
     (find-forms example)
     (map z/sexpr))
((try (if (nil? a) (hello there) (world)) (catch Throwable t)))



(map z/sexpr (find-forms example '{:contains {:form hello}, :parent {:form try}}))

(compile-path '[try :| :* {:is there}])
=> '{:contains {:is there}, :parent {:form try}}

(map z/sexpr (find-forms example '{:contains {:is there}, :parent {:form try}}))


(compile-path '[try :* :| {:is there}])
=> '{:is there, :ancestor {:form try}}

(map z/sexpr (find-forms example '{:is there, :ancestor {:form try}}))


(compile-path '[try :* :| (hello ^:% symbol?)])

(map z/sexpr (find-forms example (compile-path '[try :* :| (hello ^:% symbol?)])))

'[try :* :| hello]
(map z/sexpr (find-forms example {:form 'hello
                                  :ancestor 'try}))

'[try :* hello :|]
(map z/sexpr (find-forms example {:parent {:form 'hello
                                           :ancestor 'try}}))

'[(try & _)]
(map z/sexpr (find-forms example {:pattern '(try & _)}))

'[(try & _) :|]
(map z/sexpr (find-forms example {:parent {:pattern '(try & _)}}))

[(try & _) :| 'if]
(map z/sexpr (find-forms example {:parent {:pattern '(try & _)}
                                  :form 'if}))


[(try & _) :* 'if :* :| {:is symbol?}]
(map z/sexpr (find-forms example {:is symbol?
                                  :ancestor {:form 'if
                                             :ancestor '(try & _)}}))



'[:| defn :* catch]
=> {:form 'defn
    :contains 'catch}

'[defn :* :| catch]
=> {:is {:form 'catch
         :has-children 'catch}}

'[defn :| catch]
=> {:form 'catch
    :parent 'defn}
