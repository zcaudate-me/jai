(ns jai.query-test
  (:use midje.sweet)
  (:require [jai.query :refer :all]
            [rewrite-clj.zip :as source]))

^{:refer jai.query/match :added "0.2"}
(fact "matches the source code"
  (match (source/of-string "(+ 1 1)") '(symbol? _ _))
  => false
  
  (match (source/of-string "(+ 1 1)") '(^:% symbol? _ _))
  => true

  (match (source/of-string "(+ 1 1)") '(^:%- symbol? _ | _))
  => true

  (match (source/of-string "(+ 1 1)") '(^:%+ symbol? _ _))
  => false)

^{:refer jai.query/traverse :added "0.2"}
(fact "uses a pattern to traverse as well as to edit the form"
  
  (source/sexpr
   (traverse (source/of-string "^:a (+ () 2 3)")
             '(+ () 2 3)))
  => '(+ () 2 3)
  
  (source/sexpr
   (traverse (source/of-string "()")
             '(^:&+ hello)))
  => '(hello)
  
  (source/sexpr
   (traverse (source/of-string "()")
             '(+ 1 2 3)))
  => throws
  
  (source/sexpr
   (traverse (source/of-string "(defn hello \"world\" {:a 1} [])")
             '(defn ^:% symbol? ^:?%- string? ^:?%- map? ^:% vector? & _)))
  => '(defn hello []))

^{:refer jai.query/select :added "0.2"}
(fact "selects all patterns from a starting point"
  (map source/sexpr
   (select (source/of-string "(defn hello [] (if (try))) (defn hello2 [] (if (try)))")
           '[defn if try]))
  => '((defn hello  [] (if (try)))
       (defn hello2 [] (if (try)))))

^{:refer jai.query/modify :added "0.2"}
(fact "modifies location given a function"
  (source/root-string
   (modify (source/of-string "^:a (defn hello3) (defn hello)") ['(defn | _)]
           (fn [zloc]
             (source/insert-left zloc :hello))))
  => "^:a (defn :hello hello3) (defn :hello hello)")

^{:refer jai.query/$ :added "0.2"}
(fact "select and manipulation of clojure source code"
  
  ($ {:string "(defn hello1) (defn hello2)"} [(defn _ ^:%+ (keyword "oeuoeuoe") )])
  => '((defn hello1 :oeuoeuoe) (defn hello2 :oeuoeuoe))

  ($ {:string "(defn hello1) (defn hello2)"} [(defn _ | ^:%+ (keyword "oeuoeuoe") )])
  => '(:oeuoeuoe :oeuoeuoe))
