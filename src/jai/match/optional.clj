(ns jai.match.optional
  (:require [jai.match.common :as common]))

(defn tag-meta
  [ele out]
  (let [mele (meta ele)]
    (cond (:? mele)
          (do (swap! out update-in [:?] inc)
              (with-meta ele (assoc mele :? (:? @out))))
          
          :else ele)))

(defn pattern-seq [pattern]
  (let [out      (atom {:? -1})
        pattern (common/prewalk #(tag-meta % out) pattern)
        combos   (range (bit-shift-left 1 (inc (:? @out))))]
    (distinct
     (for [combo combos]
       (let [hide? #(if-let [num (-> % meta :?)]
                      (-> combo
                          (bit-shift-right num)
                          (mod 2)
                          (= 0)))]
         (common/remove-items pattern hide?))))))
