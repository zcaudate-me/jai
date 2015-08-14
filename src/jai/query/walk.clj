(ns jai.query.walk
  (:require [rewrite-clj.zip :as z]))

(defn matchwalk-base
  [zloc [m & more :as matchers] f recur-fn]
  (let [nloc (if (m zloc)
               (cond (empty? more)
                     (f zloc)

                     (z/down zloc)
                     (z/up (recur-fn (z/down zloc) more f recur-fn))

                     :else
                     zloc)
               zloc)
        nloc  (if-let [zdown (z/down nloc)]
                (z/up (recur-fn zdown matchers f recur-fn))
                nloc)
        nloc  (if-let [zright (z/right nloc)]
                (z/left (recur-fn zright matchers f recur-fn))
                nloc)]
    nloc))

(defn wrap-meta [walk-fn]
  (fn [zloc matchers f recur-fn]
    (if (= :meta (z/tag zloc))
      (let [nloc (z/up (walk-fn (-> zloc z/down z/right) matchers f recur-fn))]
        (if (z/right nloc)
          (walk-fn (z/right nloc) matchers f recur-fn)
          nloc))
      (walk-fn zloc matchers f recur-fn))))

(defn matchwalk [zloc matchers f]
  ((wrap-meta matchwalk-base) zloc matchers f (wrap-meta matchwalk-base)))

(defn topwalk-base
  [zloc [matcher] f recur-fn]
  (let [nloc  (if (matcher zloc)
                (f zloc)
                zloc)
        nloc  (if-let [zright (z/right nloc)]
                (z/left (recur-fn zright [matcher] f recur-fn))
                nloc)]
    nloc))

(defn topwalk [zloc [matcher] f]
  ((wrap-meta topwalk-base) zloc [matcher] f (wrap-meta topwalk-base)))
