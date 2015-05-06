(ns jai.query
  (:require [rewrite-clj.zip :as z]
            [jai.walk.position :as position]))

(comment
  (position/walk (z/of-string "(defn hello [] (prn \"hello world\"))")
                 '(defn _ ^:%? string? ^:%? map? | ^:% vector? | & _))
  
  ($ (z/of-string "(defn hello [] (prn \"hello world\"))") [(defn _ ^:%? string? ^:%? map? | ^:% vector? & _)]))
