(ns jai.match.common
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
