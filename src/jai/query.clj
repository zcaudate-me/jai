(ns jai.query
  (:require [jai.common :as common]))

(defn cursor-info [selectors]
  (let [candidates
        (->> selectors
             (keep-indexed
              (fn [i ele]
                (cond (= ele '|) [i :cursor]
                      (and (list? ele)
                           (not= (common/prepare-query ele)
                                 ele)) [i :form ele]))))]
    (case (count candidates)
      0 (if (list? (last selectors))
          [(dec (count selectors)) :form]
          [nil :cursor])
      1 (let [max      (dec (count selectors))
              [i type] (first candidates)
              _ (case type
                  :form   (if (not= i max)
                            (throw (Exception. "Form should be in the last position of the selectors")))
                  :cursor (if (= i max)
                            (throw (Exception. "Cursor cannot be in the last position of the selectors"))))]
          [i type])
      (throw (ex-info (format "There should only be one of %s in the path." ) 
                      {:candidates candidates})))))

(defn expand-all-metas [selectors]
  (common/prewalk (fn [ele] (if (instance? clojure.lang.IObj ele)
                              (common/expand-meta ele)
                              ele))
                  selectors))

(defn split-path [selectors [idx ctype]]
  (let [[up down] (cond (nil? idx)
                        [[] selectors]

                        (= :cursor ctype)
                        [(reverse (subvec selectors 0 idx))
                         (subvec selectors (inc idx) (count selectors))]

                        (= :form ctype)
                        [(reverse (subvec selectors 0 idx))
                         (subvec selectors idx (count selectors))]

                        :else (throw (Exception. "Should not be here")))]
    {:up up :down down}))

(defn jai [context selectors]
  (let [selectors  (expand-all-metas selectors)
        cursor     (cursor-info selectors)
        qselectors (mapv (fn [ele]
                           (if (list? ele)
                             (common/prepare-query ele) ele))
                         selectors)
        path-map   (split-path qselectors cursor)]
    [cursor path-map]))

(defmacro $ [context selectors]
  `(jai ~context (quote ~selectors)))


(comment
  (jai nil '[d :* (defn ^:?&- _ | & _)])


  (potential-cursors '[(defn ^:?& _ | & _)])

  (potential-cursors (expand-all-metas '[(defn ^:?& _ | & _)]))

  (potential-cursors (expand-all-metas '[(defn & _)]))

  ($ nil [(defn _ | & _)])

  (set! *print-meta* (not *print-meta*))
  )
