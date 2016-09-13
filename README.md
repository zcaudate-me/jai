# jai

[![Build Status](https://travis-ci.org/zcaudate/jai.png?branch=master)](https://travis-ci.org/zcaudate/jai)

### Installation

Add to project.clj dependencies:

```clojure
[im.chit/jai "0.2.12"]
```

All functionality is in the `jai.query` namespace:

```clojure
> (use jai.query)
```

Manipulate source code like the DOM
## Introduction
[jai](https://github.com/zcaudate/jai) makes it easy for querying and manipulation of clojure source code through an xpath/css-inspired syntax.

 - to simplify traversal and manipulation of source code
 - to provide higher level abstractions on top of [rewrite-clj](https://github.com/xsc/rewrite-clj)
 - to leverage [core.match](https://github.com/clojure/core.match)'s pattern matching

A [blog post](http://z.caudate.me/manipulate-source-code-like-the-dom/) that tells a little bit more about it.

## Motivation

As lisp code follows a tree-like structure, it is very useful to be able to have a simple language to be able to query as well as update elements of that tree. The best tool for source code manipulation is [rewrite-clj](https://www.github.com/xsc/rewrite-clj). However, it is hard to reason about the higher level concepts of code when using just a zipper for traversal.

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

### Documentation

Please see the main [documentation](http://docs.caudate.me/jai) for usage and examples.

## License

Copyright © 2015 Chris Zheng

Distributed under the MIT License
