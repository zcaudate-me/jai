# jai

[![Build Status](https://travis-ci.org/zcaudate/jai.png?branch=master)](https://travis-ci.org/zcaudate/jai)

Manipulate source code like the DOM

## Introduction

`jai` that makes it easy for querying and manipulation of clojure source code through an xpath-like syntax

 - to simplify traversal and manipulation of source code
 - to provide higher level abstractions on top of [rewrite-clj](https://github.com/xsc/rewrite-clj)
 - to leverage [core.match](https://github.com/clojure/core.match)'s pattern matching 

## Motivation

As lisp code follows a tree-like structure, it is very useful to be able to have a simple language to be able to query as well as update elements of that tree. Currently the best tool for source code manipulation is with the fantastic [rewrite-clj](https://www.github.com/xsc/rewrite-clj). However, it is hard to reason about the higher level concepts of code when using just a zipper for traversal.

`jai` is essentially a query/manipulation tool inspired by jquery and css selectors that make for easy dom manipulation and query. Instead of writing the following code with `rewrite-clj`:

```clojure
(if (and (-> zloc z/prev z/prev z/sexpr (= "defn"))
         (-> zloc z/prev z/sexpr vector?)
    (do-something zloc)
    zloc)
```

`jai` allows the same logic to be written in a much more expressive manner:

```clojure
($ zloc [(defn ^:% vector? | _)] do-something)
```

### Installation

Add to project.clj dependencies:

```clojure
[im.chit/jai "0.2.2"]
```

All functionality is in the `jai.query` namespace:

```clojure
> (use jai.query)
```

### Basics

We first define a code fragment to query on. The library currently works with strings and files.

```clojure
(def fragment {:code "(defn hello [] (println \"hello\"))\n
                      (defn world [] (if true (prn \"world\")))"})
```

Find all the `defn` forms:

```clojure
($ fragment [defn])
;=> '((defn hello [] (println "hello"))
;     (defn world [] (if true (println \"world\"))))
```

Find all the `if` forms:

```clojure
($ fragment [if])
;=> '((if true (prn "world")))

```

Find all the `defn` forms that contain an `if` form directly below it

```clojure
($ fragment [defn if])
;=> '((defn world [] (if true (prn "world"))))
```

Find all the `defn` forms that contains a prn form anywhere in its body

```clojure
($ fragment [defn :* prn])
;=> '((defn world [] (if true (prn "world"))))
```

Depth searching at specific levels can also be done, the following code performs
a search for prn at the second and third level forms below the defn:

```clojure
($ fragment [defn :2 prn])
;=> '((defn world [] (if true (prn "world"))))

($ fragment [defn :3 prn])
;=> '()
```  

### Pattern Matching

We can also use a pattern expressed using a list. Defining a pattern allows matched elements to be expressed more intuitively:

```clojure
($ fragment [(defn & _)])
;=> '((defn hello [] (println "hello"))
;     (defn world [] (if true (prn "world"))))

($ fragment [(defn hello & _)])
;=> ('(defn hello [] (println "hello")))
```

A pattern can have nested patterns:

```clojure
($ fragment [(defn world [] (if & _))])
;=> '((defn world [] (if true (prn "world"))))
```

If functions are needed, the symbols can be tagged with the `^:%`

```clojure
($ fragment [(defn world ^:% vector? ^:% list?)])
;=> '((defn world [] (if true (prn "world"))))
```

The queries are declarative and should be quite intuitive to use

```clojure
($ fragment [(_ _ _ (if :^% true? & _))])
;=> '((defn world [] (if true (prn "world"))))
```  

### Cursors

It is not very useful just selecting top-level forms. We need a way to move between the sections. This is where cursors come into picture. We can use `|` to set access to selected forms. For example, we can grab the entire top level form like this:

```clojure
($ fragment [defn println])
;=> '(defn hello [] (println "hello"))
```

But usually, the more common scenario is that we wish to perform a particular action on the `(println ...)` form. This is accessible by adding `|` in front of the `println` symbol:

```clojure
($ fragment [defn | println])
;=> '((println "hello"))
```

