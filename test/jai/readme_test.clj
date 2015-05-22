(ns jai.readme-test
  (:use midje.sweet)
  (:require [jai.query :refer :all]
            [rewrite-clj.zip :as z]))


[[:chapter {:title "Introduction"}]]

"`jai` that makes it easy for querying and manipulation of clojure source code through an `xpath`/`css`-inspired syntax

 - to simplify traversal and manipulation of source code
 - to provide higher level abstractions on top of [rewrite-clj](https://github.com/xsc/rewrite-clj)
 - to leverage [core.match](https://github.com/clojure/core.match)'s pattern matching for a more declarative syntax" 


[[:chapter {:title "Installation"}]]

"Add to `project.clj` dependencies: 

`[im.chit/jai `\"`{{PROJECT.version}}`\"`]`


All functionality is in the `jai.query` namespace:
"

(comment
  (use jai.query))

[[:chapter {:title "Usage"}]]


"We first define a code fragment to query on. The library currently works with strings and files."

(def fragment {:string "(defn hello [] (println \"hello\"))\n
                        (defn world [] (if true (prn \"world\")))"})

[[:section {:title "Basics"}]]

"Find all the `defn` forms:"

(fact 
  ($ fragment [defn])
  => '[(defn hello [] (println "hello"))
       (defn world [] (if true (prn "world")))])

"Find all the `if` forms"

(fact 
  ($ fragment [if])
  => '((if true (prn "world"))))

[[:section {:title "Path"}]]

"Find all the `defn` forms that contain an `if` form directly below it:"

(fact
  ($ fragment [defn if])
  => '[(defn world [] (if true (prn "world")))])

"Find all the `defn` forms that contains a `prn` form anywhere in its body"

(fact
  ($ fragment [defn :* prn])
  => '[(defn world [] (if true (prn "world")))])

"Depth searching at specific levels can also be done, the following code performs
a search for `prn` at the second and third level forms below the `defn`:"

(fact
  ($ fragment [defn :2 prn])
  => '[(defn world [] (if true (prn "world")))]

  ($ fragment [defn :3 prn])
  => '[])

[[:section {:title "Representation"}]]

"Instead of returning an s-expression, we can also return other represetations through specifying the `:return` value on the code. The options are `:zipper`, `:sexpr` or `:string`."

"By default, querying returns a `:sexpr` representation"
(fact
  ($ (assoc fragment :return :sexpr) [defn :* prn])
  => '[(defn world [] (if true (prn "world")))])

"String representations are useful for directly writing to file"

(fact
  ($ fragment [defn :* prn] {:return :string})
  => ["(defn world [] (if true (prn \"world\")))"])

"If more manipulation is needed, then returning a zipper allows composablity with rewrite-clj"

(fact
  (->> ($ fragment [defn :* prn] {:return :zipper})
       (map z/sexpr))
  => '[(defn world [] (if true (prn "world")))])

[[:section {:title "Cursors"}]]

"It is not very useful just selecting top-level forms. We need a way to move between the sections. This is where cursors come into picture. We can use `|` to set access to selected forms. For example, we can grab the entire top level form like this:"

(fact
  ($ fragment [defn println])
  => '[(defn hello [] (println "hello"))])

"But usually, the more common scenario is that we wish to perform a particular action on the `(println ...)` form. This is accessible by adding `\"|\"` in front of the `println` symbol:"

(fact
  ($ fragment [defn | println])
  => '[(println "hello")])

"We can see how the cursor works by drilling down into our code fragment:"

(fact
  ($ fragment [defn if prn])
  => '[(defn world [] (if true (prn "world")))]
  
  ($ fragment [| defn if prn])
  => '[(defn world [] (if true (prn "world")))]
  
  ($ fragment [defn | if prn])
  => '[(if true (prn "world"))]

  ($ fragment [defn if | prn])
  => '[(prn "world")])

[[:section {:title "Fine Grain Control"}]]

"It is not enough that we can walk to a particular form, we have to be able to control the place within the form that we wish to traverse to. "

(fact
  ($ fragment [defn (if | _ & _)])
  => '[true]

  ($ fragment [defn (if _ | _)])
  => '[(prn "world")]

  ($ fragment [defn if (prn | _)])
  => '["world"])

[[:section {:title "Pattern Matching"}]]

"We can also use a pattern expressed using a list. Defining a pattern allows matched elements to be expressed more intuitively:"

(fact

  ($ fragment [(defn & _)])
  => '[(defn hello [] (println "hello"))
       (defn world [] (if true (prn "world")))]

  ($ fragment [(defn hello & _)])
  => '[(defn hello [] (println "hello"))])

"A pattern can have nestings:"

(fact
  ($ fragment [(defn world [] (if & _))])
  => '[(defn world [] (if true (prn "world")))])


"If functions are needed, the symbols can be tagged with the a meta `^:%`"

(fact
  ($ fragment [(defn world ^:% vector? ^:% list?)])
  => '[(defn world [] (if true (prn "world")))])

"The queries are declarative and should be quite intuitive to use"

(fact
  ($ fragment [(_ _ _ (if ^:% true? & _))])
  => '[(defn world [] (if true (prn "world")))])


[[:section {:title "Insertion"}]]

"We can additionally insert elements by tagging with the `^:+` meta:"

(fact
  ($ fragment [(defn world _ ^:+ (prn "hello") & _)])
  => '[(defn world [] (prn "hello") (if true (prn "world")))])

"There are some values that do not allow metas tags (`strings`, `keywords` and `number`), in this case
the workaround is to use the `^:%+` meta and write the object as an expression to be evaluated. Note the writing `:%+` is the equivalent of writing `^{:% true :+ true}`"

(fact
  ($ fragment [(defn world _ (if true (prn ^:%+ (keyword "hello") _)))])
  => '[(defn world [] (if true (prn :hello "world")))])

"Insertions also work seamlessly with cursors:"

(fact
  ($ fragment [(defn world _ (if true | (prn ^:%+ (long 2) _)))])
  => '[(prn 2 "world")])

[[:section {:title "Deletion"}]]

"We can delete values by using the `^:-` meta tag. When used on the code fragment, we can see that the function has been mangled as the first two elements have been deleted:"

(fact
  ($ fragment [(defn ^:- world  ^:- _ & _)])
  => '[(defn (if true (prn "world")))])

"Entire forms can be marked for deletion:"

(fact
  ($ fragment [(defn world _ ^:- (if & _))])
  => '[(defn world [])])

"Deletions and insertions work quite well together. For example, below shows the replacement of the function name from `world` to `world2`:"

(fact
  ($ fragment [(defn ^:- world _ ^:+ world2 & _)])
  => '[(defn [] world2 (if true (prn "world")))])

[[:section {:title "Optional Matches"}]]

"There are certain use cases when source code has optional parameters such as a docstring or a meta map."

(fact
  ($ fragment [(defn ^:% symbol? ^:%?- string? ^:%?- map? ^:% vector? & _)])
  => '[(defn hello [] (println "hello"))
       (defn world [] (if true (prn "world")))])

"We can use optional matches to clean up certain elements within the form, such as being able to remove docstrings and meta maps if they exist."

(fact
  ($ {:string "(defn add \"adding numbers\" {:added \"0.1\"} [x y] (+ x y))"}
     [(defn ^:% symbol? ^:%?- string? ^:%?- map? ^:% vector? & _)]
     {:return :string})
  => ["(defn add [x y] (+ x y))"])


[[:chapter {:title "Working with Rewrite"}]]

"While the `$` macro is provided for global searches within a file, the `traverse` function is provided to work with the zipper library for traversal/manipulation of a form."

(fact
  (-> (z/of-string "(defn add \"adding numbers\" {:added \"0.1\"} [x y] (+ x y))")
      (traverse '(defn ^:% symbol? ^:%?- string? ^:%?- map? ^:% vector? | & _))
      (z/insert-left '(prn "add"))
      (z/up)
      (z/sexpr))
  => '(defn add [x y] (prn "add") (+ x y)))
