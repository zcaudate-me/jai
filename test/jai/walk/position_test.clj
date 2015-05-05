(ns jai.walk.position-test
  (:use midje.sweet)
  (:require [rewrite-clj.zip :as source]
            [clojure.zip :as pattern]
            [jai.walk.position :refer :all]))

(fact "sees if the node fits the expression"
  (fit-sexpr 'defn 'defn)
  => true

  (fit-sexpr 'defn symbol?)
  => true)

(defn nodes [state]
  {:source  (-> state :current :source source/sexpr)
   :pattern (-> state :current :pattern pattern/node)
   :backtrack (:backtrack state)})

(fact "turns a state into something that can be reasoned about"
  (nodes {:current   {:source (-> "(hello a b c d)" source/of-string source/down)
                      :pattern (-> '(& _) pattern/seq-zip source/down)}
          :backtrack '(:up)})
  => '{:source hello, :pattern &, :backtrack (:up)})

(fact "returns the state for traversing the `& _` pattern"  
  (-> (walk-horizontal-many
       {:current   {:source (-> "(hello a b c d)" source/of-string source/down)
                    :pattern (-> '(& _) pattern/seq-zip source/down)}
        :backtrack '(:up)})
      (nodes))
  => '{:source d, :pattern _, :backtrack (:left :left :left :left :up)})

(fact "returns the state for traversing the `& _` pattern"  
  (-> (walk-horizontal
       {:current   {:source (-> "(defn hello [])" source/of-string source/down)
                    :pattern (-> '(_ _ _) pattern/seq-zip source/down)}
        :backtrack '(:up)})
      (nodes))
  => '{:source hello, :pattern _, :backtrack (:left :up)}

  (-> (walk-horizontal
       {:current   {:source (-> "(_)" source/of-string source/down)
                    :pattern (-> '(_) pattern/seq-zip source/down)}
        :backtrack '(:up)})
      ((juxt :current :backtrack)))
  => '[{:source nil, :pattern nil}
       (:left :up)]

  (-> (walk-horizontal {:current {:source (-> "(hello [])" source/of-string source/down)
                                  :pattern (-> '(^:? defn  ^{:+ true :% true} (symbol "defn") _ _)
                                               pattern/seq-zip source/down)}
                        :level 1
                        :backtrack '(:up)})
      (nodes))
  => '{:source hello, :pattern _, :backtrack (:left :up)})

(fact "branch for pattern matching"
  (-> (walk-down
       {:current   {:source (-> "(defn hello [])" source/of-string source/down)
                    :pattern (-> '(_ _ _) pattern/seq-zip source/down)}
        :level 1
        :backtrack '(:up)})
      nodes)
  => '{:source (defn hello []), :pattern (_ _ _), :backtrack (:down :rightmost :left :left :left :up)}

  (-> (walk-down
       {:current   {:source (-> "(defn hello [])" source/of-string source/down)
                    :pattern (-> '(& _) pattern/seq-zip source/down)}
        :level 1
        :backtrack '(:up)})
      nodes)
  => '{:source (defn hello []), :pattern (& _), :backtrack (:down :rightmost :left :left :left :up)} 

  "branch for cursor behaviour"
  
  (-> (walk-down
       {:current   {:source (-> "(defn hello [])" source/of-string source/down)
                    :pattern (-> '(| & _) pattern/seq-zip source/down)}
        :level 1
        :backtrack '(:up)})
      nodes)
  => '{:source (defn hello []), :pattern (| & _), :backtrack (:down :rightmost :left :left :left)}
  
  "branch for reaching the end"
  
  (-> (walk-down
       {:current   {:source (-> "(defn)" source/of-string source/down)
                    :pattern (-> '(_) pattern/seq-zip source/down)}
        :level 1
        :backtrack '(:up)})
      nodes)
  => '{:source (defn), :pattern (_), :backtrack (:down :rightmost :left :up)})

(fact "walk replacement of items in a form"
  (-> (walk (source/of-string "(defn hello [])")
            '(| & _))
      (source/sexpr))
  => 'defn

  (-> (walk (source/of-string "(defn hello [])")
            '(_ | & _))
      (source/sexpr))
  => 'hello

  (-> (walk (source/of-string "(defn hello [])")
            '(_ _ _))
      (source/sexpr))
  => '(defn hello [])

  (-> (walk (source/of-string "(hello [])")
            '(^:+ defn _ _))
      (source/sexpr))
  => '(defn hello [])

  (-> (walk (source/of-string "(hello [])")
            '(| ^:+ defn _ _))
      (source/sexpr))
  => 'defn

  (-> (walk (source/of-string "(hello [])")
            '(^:+ defn | _ _))
      (source/sexpr))
  => 'hello

  (-> (walk (source/of-string "(hello [])")
            '(^:- hello _))
      (source/sexpr))
  => '([])

  (-> (walk (source/of-string "(hello [])")
            '(| ^:- hello _))
      (source/sexpr))
  => []
  
  (-> (walk (source/of-string "(defn hello [] (+ 1 2 3))")
            '(_ _ [] (+ | & _)))
      (source/sexpr))

  (-> (walk (source/of-string "(defn hello [a b])")
            '(_ _ [| a _]))
      (source/sexpr))
  => 'a  
  
  (-> (walk (source/of-string "(defn hello [a b] _ _)")
            '(_ _ [a | _] _ _))
      (source/sexpr))
  => 'b)
