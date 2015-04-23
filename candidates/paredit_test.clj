(ns juy.paredit-test
  (:use midje.sweet)
  (:require [juy.paredit :refer :all]))

(-> (z/of-string "(hello) (world)")
    z/right
    (z/replace {})
    ;;z/remove*
    ;;z/up
    ;;z/down
    ;;z/rightmost
    ;;(z/right)
    ;;(join)
    z/->string
    ;;z/->root-string
    )
=> "(hello world)"

(-> (z/of-string "(hello world)")
    (z/down)
    (z/right)
    (split)
    (z/insert-left 'o)
    z/->root-string)
=> "(hello) (world)"

(-> (z/of-string "a b c")
    (wrap)
    z/->root-string)
=> "(a) b c"

(-> (z/of-string "((1 2) 3 4)")
    (z/down)
    (z/down)
    (splice)
    (z/->root-string))
=> "(1 2 3 4)"


(-> (z/of-string "(a b c)")
    (z/down)
    (z/next)
    (open () 1 2 3 4)
    z/->root-string)
=> "(a (1 2 3 4) b c)"

(-> (z/of-string "(a b c)")
    (z/down)
    (open {} :a 1)
    z/->root-string)
=> "({} a b c)"

(open-round
 (z/next* (z/down )))
