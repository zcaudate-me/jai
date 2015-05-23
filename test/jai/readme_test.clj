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


[[:chapter {:title "Utilities"}]]

"These utilities are specially designed to work with `rewrite-clj`;"

(comment
  (use rewrite-clj.zip :as z))

[[:section {:title "traverse"}]]

"While the `$` macro is provided for global searches within a file, `traverse` is provided to work with the zipper library for traversal/manipulation of a form."

(fact
  (-> (z/of-string "(defn add \"adding numbers\" {:added \"0.1\"} [x y] (+ x y))")
      (traverse '(defn ^:% symbol? ^:%?- string? ^:%?- map? ^:% vector? | & _))
      (z/insert-left '(prn "add"))
      (z/up)
      (z/sexpr))
  => '(defn add [x y] (prn "add") (+ x y)))

"`traverse` can also be given a function as the third argument. This will perform some action on the location given by the cursor and then jump out again:"

(fact
  (-> (z/of-string "(defn add \"adding numbers\" {:added \"0.1\"} [x y] (+ x y))")
      (traverse '(defn ^:% symbol? ^:%?- string? ^:%?- map? ^:% vector? | & _)
                (fn [zloc] (z/insert-left zloc '(prn "add"))))
      (z/sexpr))
  => '(defn add [x y] (prn "add") (+ x y)))

"`traverse` works with metas as well, which is harder to work with using just `rewrite-clj`"

(fact
  (-> (z/of-string "(defn add [x y] ^{:text 0} (+ (+ x 1) y 1))")
      (traverse '(defn _ _ (+ (+ | x 1) y 1))
                (fn [zloc] (z/insert-left zloc '(prn "add"))))
      (z/sexpr))
  => '(defn add [x y] (+ (+ (prn "add") x 1) y 1)))

[[:section {:title "match"}]]

"a map-based syntax is provided for matching:"

(fact

  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match 'if))
  => true

  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:form 'if}))
  => true
  
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:is list?}))
  => true

  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:child {:is true}}))
  => true

  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:child {:form '+}}))
  => true)

"there are many options for matches:

 - `:fn`           match on checking function
 - `:is`           match on value or checking function
 - `:or`           match two options, done using a set
 - `:equal`        match on equivalence
 - `:type`         match on `rewrite-clj` type
 - `:meta`         match on meta tag
 - `:form`         match on first element of a form
 - `:pattern`      match on a pattern
 - `:code`         match on code

 - `:parent`       match on direct parent of element
 - `:child`        match on any child of element
 - `:first`        match on first child of element
 - `:last`         match on last child of element
 - `:nth`          match on nth child of element
 - `:nth-left`     match on nth-sibling to the left of element
 - `:nth-right`    match on nth-sibling to the right of element
 - `:nth-ancestor` match on the ancestor that is n levels higher
 - `:nth-contains` match on any contained element that is n levels lower
 - `:ancestor`     match on any ancestor
 - `:contains`     match on any contained element
 - `:sibling`      match on any sibling
 - `:left`         match on node directly to left
 - `:right`        match on node directly to right
 - `:left-of`      match on node to left
 - `:right-of`     match on node to right
 - `:left-most`    match is element is the left-most element
 - `:right-most`   match is element is the right-most element"

[[:subsection {:title ":fn"}]]

"The most general match, takes a predicate dispatching on a zipper location"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:fn (fn [zloc] (= :list (z/tag zloc)))}))
  => true)

[[:subsection {:title ":is"}]]

"The most general match, takes a value or a function"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:child {:is true}}))
  => true

  (fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:child {:is (fn [x] (instance? Boolean x))}}))
  => true))

[[:subsection {:title ":form"}]]

"By default, a symbol is evaluated as a `:form`'"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match 'if))
  => true)

"It can also be expressed explicitly:"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match '{:form if}))
  => true)

[[:subsection {:title ":or"}]]

"or style matching done using set notation"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match '#{{:form if} {:form defn}}))
  => true)

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match '#{if defn}))
  => true)

"if need arises to match a set, use the `^&` meta tag"

(fact
  (-> (z/of-string "(if #{:a :b :c} (+ 1 2) (+ 1 1))")
      (match {:child {:is '^& #{:a :b :c}}}))
  => true)

[[:subsection {:title ":and"}]]

"similar usage to :or except that vector notation is used:"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match '[if defn]))
  => false

  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match '[if {:contains 1}]))
  => true)

[[:subsection {:title ":equal"}]]

"matches sets, vectors and maps as is"

(fact
  (-> (z/of-string "(if #{:a :b :c} (+ 1 2) (+ 1 1))")
      (match {:child {:equal #{:a :b :c}}}))
  => true

  (-> (z/of-string "(if {:a 1 :b 2} (+ 1 2) (+ 1 1))")
      (match {:child {:equal {:a 1 :b 2}}}))
  => true)

[[:subsection {:title ":type"}]]

"predicate on the rewrite-clj reader type"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:type :list}))
  => true

  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:child {:type :token}}))
  => true)

[[:subsection {:title ":meta"}]]

"matches the meta on a location"

(fact
  (-> (z/down (z/of-string "^:a (+ 1 1)"))
      (match {:meta :a}))
  => true

  (-> (z/down (z/of-string "^{:a true} (+ 1 1)"))
      (match {:meta {:a true}}))
  => true)

[[:subsection {:title ":pattern"}]]

"pattern matches are done automatically with a list"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match '(if true & _)))
  => true)

"but they can be made more explicit:"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:pattern '(if true & _)}))
  => true)

[[:subsection {:title ":parent"}]]

"matches on the parent form"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (z/down)
      (z/right)
      (match {:parent 'if}))
  => true)


[[:subsection {:title ":child"}]]

"matches on any of the child forms"

(fact
  (-> (z/of-string "(if true (+ 1 2) (+ 1 1))")
      (match {:child '(+ _ 2)}))
  => true)

[[:subsection {:title ":first"}]]

"matches on the first child, can also be a vector"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (match {:child {:first '+}}))
  => true
  
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (match {:child {:first 1}}))
  => true)

[[:subsection {:title ":last"}]]

"matches on the last child element"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (match {:child {:last 3}}))
  => true

  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (match {:child {:last 1}}))
  => true)

[[:subsection {:title ":nth"}]]

"matches the nth child"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (match {:nth [1 {:equal [1 2 3]}]}))
  => true)

[[:subsection {:title ":nth-left"}]]

"matches the nth sibling to the left"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (z/rightmost)
      (match {:nth-left [2 {:equal [1 2 3]}]}))
  => true)


[[:subsection {:title ":nth-right"}]]

"matches the nth sibling to the right"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (match {:nth-right [1 {:equal [1 2 3]}]}))
  => true)

[[:subsection {:title ":nth-ancestor"}]]

"matches the nth ancestor in the hierarchy"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (z/right)
      (z/down)
      (match {:nth-ancestor [2 {:form 'if}]}))
  => true)

[[:subsection {:title ":nth-contains"}]]

"matches the nth level children"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (match {:nth-contains [2 {:is 3}]}))
  => true)

[[:subsection {:title ":ancestor"}]]

"matches any ancestor"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (z/right)
      (z/down)
      (match {:ancestor 'if}))
  => true)

[[:subsection {:title ":contains"}]]

"matches the any subelement contained by the element"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (match {:contains 3}))
  => true)

[[:subsection {:title ":sibling"}]]

"matches any sibling"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (match {:sibling {:form '+}}))
  => true)

[[:subsection {:title ":left"}]]

"matches element to the left"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (z/right)
      (match {:left {:is 'if}}))
  => true)

[[:subsection {:title ":right"}]]

"matches element to the right"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (match {:right {:is [1 2 3]}}))
  => true)

[[:subsection {:title ":left-of"}]]

"matches any element to the left"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (z/rightmost)
      (match {:left-of {:is [1 2 3]}}))
  => true)

[[:subsection {:title ":right-of"}]]

"matches any element to the right"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (match {:right-of '(+ 1 1)}))
  => true)

[[:subsection {:title ":left-most"}]]

"is the left-most element"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (match {:left-most true}))
  => true)

[[:subsection {:title ":right-most"}]]

"is the right-most element"

(fact
  (-> (z/of-string "(if [1 2 3] (+ 1 2) (+ 1 1))")
      (z/down)
      (z/rightmost)
      (match {:right-most true}))
  => true)
