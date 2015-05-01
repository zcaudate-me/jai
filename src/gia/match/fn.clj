(ns gia.match.fn
  (:require [clojure.core.match :as match]))

(defmethod match/emit-pattern clojure.lang.Fn
  [pat] 
  (throw (ex-info "Cannot emit pattern for raw clojure functions, please use vars or prefix with ^:%" {:value pat})))

(defmethod match/emit-pattern clojure.lang.Var
  [pat] pat)

(defmethod match/to-source clojure.lang.Var
  [pat ocr]
  (if (fn? @pat)
    `(try ((deref ~pat) ~ocr)
          (catch Throwable t# false))
    `(= (deref ~pat) ~ocr)))

(defmethod match/groupable? [clojure.lang.Var clojure.lang.Var]
  [a b]
  (= a b))
