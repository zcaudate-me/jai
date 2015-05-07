(ns jai.match.optional
  (:require [jai.match.common :as common]))

(defn tag-meta
  [ele out]
  (let [mele (meta ele)]
    (cond (:? mele)
          (do (swap! out update-in [:?] inc)
              (with-meta ele (assoc mele :? (:? @out))))
          
          :else ele)))

(defn remove-element [ele combo]
  (if-let [num (-> ele meta :?)]
    (if (-> combo (bit-shift-right num) (mod 2) (= 0))
      ::null
      ele)
    ele))

(defn remove-nulls [ele]
  (cond (list? ele)   (with-meta (apply list (filter #(not= ::null %) ele))
                        (meta ele))
        (vector? ele) (with-meta (filterv #(not= ::null %) ele)
                        (meta ele))
        :else ele))

(defn pattern-seq [template]
  (let [out      (atom {:? -1})
        template (common/prewalk #(tag-meta % out) template)
        combos   (range (bit-shift-left 1 (inc (:? @out))))]
    (distinct
     (for [combo combos]
       (->> template
            (common/prewalk #(remove-element % combo))
            (common/prewalk remove-nulls))))))
