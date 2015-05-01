(ns jai.query.match-test
  (:use midje.sweet)
  (:require [jai.query.match :refer :all]
            [rewrite-clj.zip :as z]))

^{:refer jai.query.match/p-fn :added "0.1"}
(fact "takes a predicate function to check the state of the zipper"
  ((p-fn (fn [x]
           (-> (z/node x) (.tag) (= :token))))
   (z/of-string "defn"))
  => true)

^{:refer jai.query.match/p-is :added "0.1"}
(fact "checks if node is equivalent, does not meta into account"
  ((p-is 'defn) (z/of-string "defn"))
  => true

  ((p-is '^{:a 1} defn) (z/of-string "defn"))
  => true
  
  ((p-is 'defn) (z/of-string "is"))
  => false

  ((p-is '(defn & _)) (z/of-string "(defn x [])"))
  => false)

^{:refer jai.query.match/p-equal :added "0.1"}
(fact "checks if the node is equivalent, takes meta into account"
  ((p-equal '^{:a 1} defn) (z/of-string "defn"))
  => false

  ((p-equal '^{:a 1} defn) (z/of-string "^{:a 1} defn"))
  => true

  ((p-equal '^{:a 1} defn) (z/of-string "^{:a 2} defn"))
  => false)

^{:refer jai.query.match/p-meta :added "0.1"}
(fact "checks if meta is the same"
  ((p-meta {:a 1}) (z/of-string "^{:a 1} defn"))
  => true
  
  ((p-meta {:a 1}) (z/of-string "^{:a 2} defn"))
  => false)

^{:refer jai.query.match/p-type :added "0.1"}
(fact "check on the type of element"
  ((p-type :token) (z/of-string "defn"))
  => true
  
  ((p-type :token) (z/of-string "^{:a 1} defn"))
  => true)

^{:refer jai.query.match/p-form :added "0.1"}
(fact "checks if it is a form with the symbol as the first element"
  ((p-form 'defn) (z/of-string "(defn x [])"))
  => true
  ((p-form 'let) (z/of-string "(let [])"))
  => true)

^{:refer jai.query.match/p-pattern :added "0.1"}
(fact "checks if the form matches a particular pattern"
  ((p-pattern '(defn ^:% symbol? & _)) (z/of-string "(defn ^{:a 1} x [])"))
  => true

  ((p-pattern '(defn ^:% symbol? ^:%? string? [])) (z/of-string "(defn ^{:a 1} x [])"))
  => true
  )

^{:refer jai.query.match/p-code :added "0.1"}
(fact "checks if the form matches a string in the form of a regex expression"
  ((p-code #"defn") (z/of-string "(defn ^{:a 1} x [])"))
  => true)

^{:refer jai.query.match/p-and :added "0.1"}
(fact "takes multiple predicates and ensures that all are correct"
  ((p-and (p-code #"defn")
          (p-type :token)) (z/of-string "(defn ^{:a 1} x [])"))
  => false

  ((p-and (p-code #"defn")
          (p-type :list)) (z/of-string "(defn ^{:a 1} x [])"))
  => true)

^{:refer jai.query.match/p-or :added "0.1"}
(fact "takes multiple predicates and ensures that at least one is correct"
  ((p-or (p-code #"defn")
          (p-type :token)) (z/of-string "(defn ^{:a 1} x [])"))
  => true

  ((p-or (p-code #"defn")
          (p-type :list)) (z/of-string "(defn ^{:a 1} x [])"))
  => true)

^{:refer jai.query.match/p-parent :added "0.1"}
(fact "checks that the parent of the element contains a certain characteristic"
  ((p-parent 'defn) (-> (z/of-string "(defn x [])") z/next z/next))
  => true

  ((p-parent {:parent 'if}) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => true
  
  ((p-parent {:parent 'if}) (-> (z/of-string "(if (= x y))") z/down))
  => false)

^{:refer jai.query.match/p-child :added "0.1"}
(fact "checks that there is a child of a container that has a certain characteristic"
  ((p-child {:form '=}) (z/of-string "(if (= x y))"))
  => true

  ((p-child '=) (z/of-string "(if (= x y))"))
  => false)

^{:refer jai.query.match/p-first :added "0.1"}
(fact "checks that the first element of the container has a certain characteristic"
  ((p-first 'defn) (-> (z/of-string "(defn x [])")))
  => true
  
  ((p-first 'x) (-> (z/of-string "[x y z]")))
  => true

  ((p-first 'x) (-> (z/of-string "[y z]")))
  => false)

^{:refer jai.query.match/p-last :added "0.1"}
(fact "checks that the last element of the container has a certain characteristic"
  ((p-last 1) (-> (z/of-string "(defn [] 1)")))
  => true
  
  ((p-last 'z) (-> (z/of-string "[x y z]")))
  => true

  ((p-last 'x) (-> (z/of-string "[y z]")))
  => false)


^{:refer jai.query.match/p-nth :added "0.1"}
(fact "checks that the last element of the container has a certain characteristic"
  ((p-nth [0 'defn]) (-> (z/of-string "(defn [] 1)")))
  => true
  
  ((p-nth [2 'z]) (-> (z/of-string "[x y z]")))
  => true

  ((p-nth [2 'x]) (-> (z/of-string "[y z]")))
  => false)


^{:refer jai.query.match/p-nth-left :added "0.1"}
(fact "checks that the last element of the container has a certain characteristic"
  ((p-nth-left [0 'defn]) (-> (z/of-string "(defn [] 1)") z/down))
  => true

  ((p-nth-left [1 ^:% vector?]) (-> (z/of-string "(defn [] 1)") z/down z/rightmost))
  => true)


^{:refer jai.query.match/p-nth-right :added "0.1"}
(fact "checks that the last element of the container has a certain characteristic"
  ((p-nth-right [0 'defn]) (-> (z/of-string "(defn [] 1)") z/down))
  => true
  
  ((p-nth-right [1 ^:% vector?]) (-> (z/of-string "(defn [] 1)") z/down))
  => true)



^{:refer jai.query.match/p-contains :added "0.1"}
(fact "checks that any element (deeply nested also) of the container matches"
  ((p-contains '=) (z/of-string "(if (= x y))"))
  => true

  ((p-contains 'x) (z/of-string "(if (= x y))"))
  => true)

^{:refer jai.query.match/p-ancestor :added "0.1"}
(fact "checks that any parent container matches"
  ((p-ancestor {:form 'if}) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => true
  ((p-ancestor 'if) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => true)

^{:refer jai.query.match/p-sibling :added "0.1"}
(fact "checks that any element on the same level has a certain characteristic"
  ((p-sibling '=) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => false
  
  ((p-sibling 'x) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => true)

^{:refer jai.query.match/p-left :added "0.1"}
(fact "checks that the element on the left has a certain characteristic"
  ((p-left '=) (-> (z/of-string "(if (= x y))") z/down z/next z/next z/next))
  => true
  
  ((p-left 'if) (-> (z/of-string "(if (= x y))") z/down z/next))
  => true)

^{:refer jai.query.match/p-right :added "0.1"}
(fact "checks that the element on the right has a certain characteristic"
  ((p-right 'x) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => true
  
  ((p-right {:form '=}) (-> (z/of-string "(if (= x y))") z/down))
  => true)

^{:refer jai.query.match/p-left-of :added "0.1"}
(fact "checks that any element on the left has a certain characteristic"
  ((p-left-of '=) (-> (z/of-string "(= x y)") z/down z/next))
  => true
  
  ((p-left-of '=) (-> (z/of-string "(= x y)") z/down z/next z/next))
  => true)

^{:refer jai.query.match/p-right-of :added "0.1"}
(fact "checks that any element on the right has a certain characteristic"
  ((p-right-of 'x) (-> (z/of-string "(= x y)") z/down))
  => true
  
  ((p-right-of 'y) (-> (z/of-string "(= x y)") z/down))
  => true

  ((p-right-of 'z) (-> (z/of-string "(= x y)") z/down))
  => false)


^{:refer jai.query.match/p-left-most :added "0.1"}
(fact "checks that any element on the right has a certain characteristic"
  ((p-left-most true) (-> (z/of-string "(= x y)") z/down))
  => true
  
  ((p-left-most true) (-> (z/of-string "(= x y)") z/down z/next))
  => false)


^{:refer jai.query.match/p-right-most :added "0.1"}
(fact "checks that any element on the right has a certain characteristic"
  ((p-right-most true) (-> (z/of-string "(= x y)") z/down z/next))
  => false
  
  ((p-right-most true) (-> (z/of-string "(= x y)") z/down z/next z/next))
  => true)
