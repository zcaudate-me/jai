(ns juy.test-match
  ;(:use midje.sweet)
  (:require [juy.match :refer :all]))

(match-template
 '(defn id [x & more] x)
 '(defn id [x ^:- & _] & _))

(match-template
 '(defn id [x] x)
 '(defn ^:+ symbol? [& _] & _))

(match-template
 '(defn id [x] x)
 '(defn _  [& _] _))

(match-template
 '(defn id [x] x)
 '(defn _  [& _] _))

(match-template
 '(defn id [x]
    [(if true false)
     (do 1 2 3)])

 '(defn id _
    [(if _ & _)
     (do _ 2 _ & _)]))

(match-template
 "oeuoeuo"
 "oeuoeuo")

(match-template
 "oeuoeuo"
 string?)

(match-template
 '(defn is [x] hello)
 '(defn is ^:+ vector? & _))
