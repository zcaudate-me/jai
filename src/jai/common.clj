(ns jai.common
  (:require [clojure.string :as string]
            [clojure.walk :as walk]))

(defn any [x] true)

(defn none [x] false)

(defn expand-meta [ele]
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

 (defn cursor? [ele] (= '| ele))

 (defn insertion? [ele] (-> ele meta :+))

 (defn deletion? [ele] (-> ele meta :-))

(defn wrap-keep-meta [f]
  (fn [inner outer form]
    (let [obj (f inner outer form)]
      (if (and (instance? clojure.lang.IObj form)
               (instance? clojure.lang.IObj obj))
        (with-meta obj (meta form))
        obj))))

(defn prewalk
  [f form]
  ((wrap-keep-meta walk/walk) (partial prewalk f) identity (f form)))

(defn remove-null [ele]
  (cond (list? ele)   (with-meta (apply list (filter #(not= ::null %) ele))
                        (meta ele))
        (vector? ele) (with-meta (filterv #(not= ::null %) ele)
                        (meta ele))
        :else ele))

(defn mark-null [pred]
  (fn [ele]
    (if (pred ele) ::null ele)))

(defn remove-items [pred pattern]
  (->> pattern
       (prewalk (mark-null pred))
       (prewalk remove-null)))

(defn prepare-deletion [pattern]
  (->> pattern
       (remove-items cursor?)
       (remove-items insertion?)))

(defn prepare-insertion [pattern]
 (->> pattern
      (remove-items cursor?)
      (remove-items deletion?)))

(defn prepare-query [pattern]
 (->> pattern
      (remove-items cursor?)
      (remove-items deletion?)
      (remove-items insertion?)))

(defn find-index
  ([pred seq]
   (find-index pred seq 0))
  ([pred [x & more :as seq] idx]
   (cond (empty? seq) nil
         (pred x)     idx
         :else (recur pred more (inc idx)))))

(defn finto [to from]
  (cond (list? to)
        (into to (reverse from))
        :else (into to from)))
