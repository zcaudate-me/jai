(ns juy.query.pattern.regex
  (:require [clojure.core.match :as match]))

(defmethod match/emit-pattern java.util.regex.Pattern
  [pat]
  pat)

(defmethod match/to-source java.util.regex.Pattern
  [pat ocr]
  `(cond (string? ~ocr)
         (re-find ~pat ~ocr)

         (instance? java.util.regex.Pattern ~ocr)
         (= (.pattern ~pat)
            (.pattern ~ocr))))

(defmethod match/groupable? [java.util.regex.Pattern java.util.regex.Pattern]
  [a b]
  (and (= (.pattern a) (.pattern b))
       (= (.flags a) (.flags b))))
