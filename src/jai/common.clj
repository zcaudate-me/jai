(ns jai.common
  (:require [clojure.string :as string]
            [clojure.walk :as walk]))

(defn any
  "returns true for any value
  (any nil) => true
  (any '_) => true"
  {:added "0.2"}
  [x] true)

(defn none
  "returns false for any value
  (none nil) => false
  (none '_) => false"
  {:added "0.2"}
  [x] false)

(defn expand-meta
  "separates out the meta into individual flags
  (meta (expand-meta ^:? ()))
  => {:? true}
  (meta (expand-meta ^:+%? ()))
  => {:+ true, :? true, :% true}"
  {:added "0.2"}
  [ele]
  (->> (meta ele)
       (keys)
       (map name)
       (apply str)
       (#(string/split % #""))
       (map keyword)
       (set)
       (#(-> %
             (zipmap (repeat true))
             (select-keys [:% :? :& :- :+])))
       (with-meta ele)))

(defn cursor?
  "checks if element is `|`
  (cursor? '|) => true
  (cursor? '_) => false"
  {:added "0.2"}
  [ele] (= '| ele))

(defn insertion?
  "checks if element has an insert meta
  (insertion? '^:+ a) => true
  (insertion? 'a) => false"
  {:added "0.2"}
  [ele] (or (-> ele meta :+) false))

(defn deletion?
  "checks if element has a delete meta
  (deletion? '^:- a) => true
  (deletion? 'a) => false"
  {:added "0.2"}
  [ele] (or (-> ele meta :-) false))

(defn- wrap-keep-meta [f]
  (fn [inner outer form]
    (let [obj (f inner outer form)]
      (if (and (instance? clojure.lang.IObj form)
               (instance? clojure.lang.IObj obj))
        (with-meta obj (meta form))
        obj))))

(defn prewalk
  [f form]
  ((wrap-keep-meta walk/walk) (partial prewalk f) identity (f form)))

(defn- remove-null [ele]
  (cond (list? ele)   (with-meta (apply list (filter #(not= ::null %) ele))
                        (meta ele))
        (vector? ele) (with-meta (filterv #(not= ::null %) ele)
                        (meta ele))
        :else ele))

(defn- mark-null [pred]
  (fn [ele]
    (if (pred ele) ::null ele)))

(defn remove-items
  "removes items from a form matching the predicate
  (remove-items #(= 1 %) '(1 2 3 4))
  => '(2 3 4)

  (remove-items #(= 1 %) '(1 (1 (1 (1)))))
  => '(((())))"
  {:added "0.2"}
  [pred pattern]
  (->> pattern
       (prewalk (mark-null pred))
       (prewalk remove-null)))

(defn prepare-deletion
  "removes extraneous symbols for deletion walk
  (prepare-deletion '(+ a 2))
  => '(+ a 2)

  (prepare-deletion '(+ ^:+ a | 2))
  => '(+ 2)"
  {:added "0.2"}
  [pattern]
  (->> pattern
       (remove-items cursor?)
       (remove-items insertion?)))

(defn prepare-insertion
  "removes extraneous symbols for deletion walk
  (prepare-insertion '(+ a 2))
  => '(+ a 2)

  (prepare-insertion '(+ ^:+ a | ^:- b 2))
  => '(+ a 2)"
  {:added "0.2"}
  [pattern]
 (->> pattern
      (remove-items cursor?)
      (remove-items deletion?)))

(defn prepare-query [pattern]
 (->> pattern
      (remove-items cursor?)
      (remove-items deletion?)
      (remove-items insertion?)))

(defn find-index
  "returns the index of the first occurrence
  (find-index #(= 2 %) '(1 2 3 4))
  => 1"
  {:added "0.2"}
  ([pred seq]
   (find-index pred seq 0))
  ([pred [x & more :as seq] idx]
   (cond (empty? seq) nil
         (pred x)     idx
         :else (recur pred more (inc idx)))))

(defn finto
  "into but the right way for lists
  (finto () '(1 2 3))
  => '(1 2 3)"
  {:added "0.2"}
  [to from]
  (cond (list? to)
        (into to (reverse from))
        :else (into to from)))
