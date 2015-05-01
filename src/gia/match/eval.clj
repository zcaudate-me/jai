(ns jai.match.eval
  (:require [clojure.core.match :as match]))

(defrecord EvaluationPattern [expression])

(defn eval-pattern [expression]
  (EvaluationPattern. expression))

(defmethod match/emit-pattern EvaluationPattern
  [pat] pat)

(defmethod match/to-source EvaluationPattern
  [pat ocr]
  (let [v (eval (:expression pat))]
    (cond (fn? v)
          `(try (~(:expression pat) ~ocr)
                (catch Throwable t# false))
          :else (match/to-source v))))

(defmethod match/groupable? [EvaluationPattern EvaluationPattern]
  [a b]
  (= (:template a)
     (:template b)))
