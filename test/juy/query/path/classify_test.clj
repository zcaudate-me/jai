(ns juy.query.path.classify-test
  (:use midje.sweet)
  (:require [juy.query.path.classify :refer :all]))

{:refer juy.query.path.classify/normalise-meta :added "0.1"}
(fact "converts "
  (meta (normalise-meta '^:? _))
  => {:? true}
  
  (meta (normalise-meta '^:#$? _))
  => {:? true, :$ true, :# true})


{:refer juy.query.path.classify/classify :added "0.1"}
(fact "converts a vector into a map of elements"

  (classify '[defn])
  => '{:down [defn]}

  (classify '[defn | if])
  => '{:down [if] :up [defn]}

  (classify '[defn if |])
  => '{:up [if defn]}

  (classify '[defn if [^:# |]])
  => '{:up [if defn] :modifiers {:# true}}

  (classify '[defn if [^:# _ _]])
  => '{:up [if defn] :left [_ ^:# _]}

  (classify '[defn if [^:# _ _] then])
  => '{:up [if defn] :left [_ ^:# _] :down [then]}
  
  (classify '[defn if [^:# _ _ | ^:% vector?]])
  => '{:up [if defn] :left [_ ^:# _] :right [^:% vector?]}

  (classify '[defn :2 if [_ _] hello])
  => '{:up [if :2 defn] :left [_ _] :down [hello]})

