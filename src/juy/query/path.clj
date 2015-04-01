(ns juy.query.path
  (:require [rewrite-clj.zip :as z]
            [hara.common.checks :refer [hash-map? regex?]]
            [juy.query.match :as match]
            [juy.query.walk :as walk]))

(defn find-cursor-index [path]
  (let [cursors (keep-indexed (fn [i e] (if (= e :|) i)) path)]
    (case (count cursors)
        0 -1
        1 (first cursors)
        (throw (ex-info "There should only be one `:|` in the path:" {:path path
                                                                      :cursors cursors})))))
(defn group-items
  ([path] (group-items path []))
  ([[x y & xs :as more] out]
   (cond (empty? more) out

         (= :* y)
         (recur xs (conj out [:* x]))

         (= :> x)
         (recur xs (conj out [:> nil]))
         
         :else
         (recur (rest more) (conj out [:> x])))))

(defn compile-path-template [template]
  (cond (:% (meta template)) (compile-path-template (eval template))
        (fn? template)     {:is template}
        (map? template)    template
        (list? template)   {:pattern template}
        (symbol? template) {:form template}
        :else {:is template}))

(defn compile-before-directives
  ([ds]
   (reduce (fn [out [j template]]
             (let [k (case j :* :ancestor :> :parent)]
               {k (merge out (compile-path-template template))}))
           {}
           ds)))

(defn compile-after-directives
  ([ds] (compile-after-directives ds {}))
  ([[[j template] & more :as ds] out]
   (cond (empty? ds) out

         :else
         (recur more
                (let [k (case j :* :contains :> :child)
                      m (merge out (compile-path-template template))]
                  (if (and (empty? more)
                           (= j :>))
                    m
                    {k m}))))))

(defn compile-path [path]
  (let [idx (find-cursor-index path)
        len (count path)
        [before after] (if (= idx -1)
                         [[] (-> path reverse group-items)]
                         [(-> (subvec path 0 idx) group-items)
                          (-> (subvec path (inc idx) len) reverse group-items)])]
    (merge (compile-before-directives before)
           (compile-after-directives after))))
