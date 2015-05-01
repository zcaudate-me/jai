(ns gia.query.pattern.fn
  (:require [clojure.core.match :as match]))

(defmethod match/emit-pattern clojure.lang.Fn
  [pat]
  pat)

(defmethod match/to-source clojure.lang.Fn
  [pat ocr]
  `(try (~pat ~ocr)
        (catch Throwable t# false)))

(defmethod match/groupable? [clojure.lang.Fn clojure.lang.Fn]
  [a b]
  false)