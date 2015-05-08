(ns jai.query.walk
  (:require [rewrite-clj.zip :as z]))

(defn prewalk
  [zloc m f]
  (let [nloc  (if (m zloc)
                (f zloc)
                zloc)
        nloc  (if-let [zdown (z/down nloc)]
                (z/up (prewalk zdown m f))
                nloc)
        nloc  (if-let [zright (z/right nloc)]
                (z/left (prewalk zright m f))
                nloc)]
    nloc))

(defn postwalk
  [zloc m f]
  (let [nloc  (if-let [zdown (z/down zloc)]
                (z/up (postwalk zdown m f))
                zloc)
        nloc  (if-let [zright (z/right nloc)]
                (z/left (postwalk zright m f))
                nloc)
        nloc  (if (m nloc)
                (f nloc)
                nloc)]
    nloc))

(defn matchwalk
  [zloc [m & more :as matchers] f]
  (let [nloc (if (m zloc)
               (cond (empty? more)
                     (f zloc)

                     (z/down zloc)
                     (z/up (matchwalk (z/down zloc) more f))

                     :else
                     zloc)
               zloc)
        nloc  (if-let [zdown (z/down zloc)]
                (z/up (matchwalk zdown matchers f))
                zloc)
        nloc  (if-let [zright (z/right nloc)]
                (z/left (matchwalk zright matchers f))
                nloc)]
    nloc))

(defn levelwalk
  [zloc [m & more :as matchers] f]
  (let [nloc (if (m zloc)
               (cond (empty? more)
                     (f zloc)

                     (z/down zloc)
                     (z/up (levelwalk (z/down zloc) more f))

                     :else
                     zloc)
               zloc)
        nloc  (if-let [zright (z/right nloc)]
                (z/left (levelwalk zright matchers f))
                nloc)]
    nloc))
