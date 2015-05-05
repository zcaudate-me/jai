(ns jai.common
  (:require [clojure.string :as string]))

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
