(ns jai.walk.position-advanced-test
  (:use midje.sweet)
  (:require [rewrite-clj.zip :as source]
            [clojure.zip :as pattern]
            [jai.walk.position :refer :all]))

(fact "Some advanced uses of add"
  (-> (walk (source/of-string "(defn hello [a] (if (not (nil? a)) (println (str \"Hello \" a))))")
            '(defn hello [^:+ b _] (if (not (nil? ^:+ b a)) _) _))
      (source/sexpr))
  => '(defn hello [b a] (if (not (nil? b a)) (println (str "Hello " a))))
  
  (-> (walk (source/of-string "(defn hello [a] (if (not (nil? a)) (println (str \"Hello \" a))))")
            '(defn hello [^:+ b a] (if (not (nil? ^:+ b | a)) _) _))
      (source/sexpr))
  => 'a

  (-> (walk (source/of-string "(defn hello [a] (if (not (nil? a)) (println (str \"Hello \" a))))")
            '(defn hello [^:+ b a] (if | (not (nil? ^:+ b a)) _)))
      (source/sexpr))
  => '(not (nil? b a))

  (-> (walk (source/of-string "(defn hello [a] (if (not (nil? a)) (println (str \"Hello \" a))))")
            '(defn hello [^:+ b a] (if (not (nil? ^:+ b a)) | _)))
      (source/sexpr))
  => '(println (str "Hello " a))


  (-> (walk (source/of-string "(defn hello \"a\" {:a 1} [] (str))")
          '(_ _
              ^{:% true :- true} string?
              ^{:% true :+ true} (str "Hello There")
              ^{:% true :- true} map?
              ^:+ {:b 2} [] & _))
      (source/sexpr))
  => '(defn hello "Hello There" {:b 2} [] (str))
  
  (-> (walk (source/of-string "(defn hello \"a\" {:a 1} [] (str))")
          '(_ _
              ^{:% true :- true} string?
              ^{:% true :+ true} (str "Hello There")
              ^{:% true :- true} map?
              ^:+ {:b 2} [] | & _))
      (source/sexpr))
  => '(str))


