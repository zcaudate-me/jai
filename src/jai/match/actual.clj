(ns jai.match.actual
  (:require [clojure.core.match :as match]))

(defrecord ActualPattern [expression])

(defn actual-pattern [expression]
  (ActualPattern. expression))

(defmethod match/emit-pattern ActualPattern
  [pat] pat)

(defmethod match/to-source ActualPattern
  [pat ocr]
  (let [v (:expression pat)
        v (if (-> v meta :%) (eval v) v)]
    `(= ~v ~ocr)))

(defmethod match/groupable? [ActualPattern ActualPattern]
  [a b]
  (and (= (:expression a)
          (:expression b))
       (= (-> a meta :%)
          (-> b meta :%))))
