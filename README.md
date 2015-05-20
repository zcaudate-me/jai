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


