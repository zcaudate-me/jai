(ns juy.query.path.classify-test
  (:use midje.sweet)
  (:require [juy.query.path.classify :refer :all]))

{:refer juy.query.path.classify/normalise-meta :added "0.1"}
(fact "converts a symbol to have seperated meta tags"
  (meta (normalise-meta '^:? _))
  => {:? true}
  
  (meta (normalise-meta '^:#$? _))
  => {:? true, :$ true, :# true})


{:refer juy.query.path.classify/classify :added "0.1"}
(fact "converts a vector into a map of elements"

  (classify '[defn])
  => '{:down [defn] :modifiers {:direction :vertical}}

  (classify '[defn | if])
  => '{:down [if] :up [defn] :modifiers {:direction :vertical}}

  (classify '[defn if |])
  => '{:up [if defn] :modifiers {:direction :vertical}}

  (classify '[defn if [^:# |]])
  => '{:up [if defn] :modifiers {:# true :direction :horizontal}}

  (classify '[defn if [^:# _ _]])
  => '{:up [if defn] :left [_ ^:# _] :modifiers {:direction :horizontal}}

  (classify '[defn if [^:# _ _] then])
  => '{:up [if defn] :left [_ ^:# _] :down [then] :modifiers {:direction :horizontal}}
  
  (classify '[defn if [^:# _ _ | ^:% vector?]])
  => '{:up [if defn] :left [_ ^:# _] :right [^:% vector?] :modifiers {:direction :horizontal}}

  (classify '[defn :2 if [_ _] hello])
  => '{:up [if :2 defn] :left [_ _] :down [hello] :modifiers {:direction :horizontal}})

