(ns juy.query.match-test
  (:use midje.sweet)
  (:require [juy.query.match :refer :all]
            [rewrite-clj.zip :as z]))

(fact "p-fn"
  ((p-fn (fn [x]
           (-> (z/node x) (.tag) (= :token))))
   (z/of-string "defn"))
  => true)

(fact "p-is"
  ((p-is 'defn) (z/of-string "defn"))
  => true

  ((p-is 'defn) (z/of-string "is"))
  => false

  ((p-is '(defn & _)) (z/of-string "(defn x [])"))
  => false)

(fact "p-equal"
  ((p-equal '^{:a 1} defn) (z/of-string "defn"))
  => false

  ((p-equal '^{:a 1} defn) (z/of-string "^{:a 1} defn"))
  => true

  ((p-equal '^{:a 1} defn) (z/of-string "^{:a 2} defn"))
  => false)

(fact "p-meta"
  ((p-meta {:a 1}) (z/of-string "^{:a 1} defn"))
  => true
  
  ((p-meta {:a 1}) (z/of-string "^{:a 2} defn"))
  => false)

(fact "p-type"
  ((p-type :token) (z/of-string "defn"))
  => true
  
  ((p-type :token) (z/of-string "^{:a 1} defn"))
  => true)

(fact "p-form"
  ((p-form 'defn) (z/of-string "(defn x [])"))
  => true
  ((p-form 'let) (z/of-string "(let [])"))
  => true)

(fact "p-pattern"
  ((p-pattern '(defn ^:% symbol? & _)) (z/of-string "(defn ^{:a 1} x [])"))
  => true)

(fact "p-code"
  ((p-code #"defn") (z/of-string "(defn ^{:a 1} x [])"))
  => true)

(fact "p-and"
  ((p-and (p-code #"defn")
          (p-type :token)) (z/of-string "(defn ^{:a 1} x [])"))
  => false

  ((p-and (p-code #"defn")
          (p-type :list)) (z/of-string "(defn ^{:a 1} x [])"))
  => true)


(fact "p-or"
  ((p-or (p-code #"defn")
          (p-type :token)) (z/of-string "(defn ^{:a 1} x [])"))
  => true

  ((p-or (p-code #"defn")
          (p-type :list)) (z/of-string "(defn ^{:a 1} x [])"))
  => true)

(fact "p-parent"
  ((p-parent 'defn) (-> (z/of-string "(defn x [])") z/next z/next))
  => true

  ((p-parent {:parent 'if}) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => true
  
  ((p-parent {:parent 'if}) (-> (z/of-string "(if (= x y))") z/down))
  => false)

(fact "p-first"
  ((p-first 'defn) (-> (z/of-string "(defn x [])")))
  => true
  
  ((p-first 'x) (-> (z/of-string "[x y z]")))
  => true

  ((p-first 'x) (-> (z/of-string "[y z]")))
  => false)

(fact "p-child"
  ((p-child {:form '=}) (z/of-string "(if (= x y))"))
  => true

  ((p-child '=) (z/of-string "(if (= x y))"))
  => false)

(fact "p-contains"
  ((p-contains '=) (z/of-string "(if (= x y))"))
  => true

  ((p-contains 'x) (z/of-string "(if (= x y))"))
  => true)

(fact "p-ancestor"
  ((p-ancestor {:form 'if}) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => true
  ((p-ancestor 'if) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => true)

(fact "p-left"
  ((p-left '=) (-> (z/of-string "(if (= x y))") z/down z/next z/next z/next))
  => true
  
  ((p-left 'if) (-> (z/of-string "(if (= x y))") z/down z/next))
  => true)

(fact "p-right"
  ((p-right 'x) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => true
  
  ((p-right {:form '=}) (-> (z/of-string "(if (= x y))") z/down))
  => true)

(fact "p-sibling"
  ((p-sibling '=) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => false
  
  ((p-sibling 'x) (-> (z/of-string "(if (= x y))") z/down z/next z/next))
  => true)
