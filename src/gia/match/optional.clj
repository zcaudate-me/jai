(ns gia.match.optional
  (:require [clojure.walk :as walk]))

(defn expand-meta
  [ele out]
  (let [mele (meta ele)]
    (cond (:%? mele)
          (expand-meta (with-meta ele (-> (dissoc mele :%?)
                                          (assoc :% true :? true)))
                       out)
          (:? mele)
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
  (cond (list? ele) (filter #(not= ::null %) ele)
        (vector? ele) (filterv #(not= ::null %) ele)
        :else ele))

(defn pattern-seq [template]
  (let [out      (atom {:? -1})
        template (walk/postwalk #(expand-meta % out) template)
        combos   (range (bit-shift-left 1 (inc (:? @out))))]
    (->> template
         (walk/postwalk #(remove-element % combo))
         (walk/prewalk remove-nulls)
         (for [combo combos])
         (distinct))))
