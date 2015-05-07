(ns jai.query.path
  (:require [jai.query.path.classify :as classify]
            [clojure.walk :as walk]))

(def moves
  {:step   {:up     :parent
            :down   :child}
   :multi  {:up     :ancestor
            :down   :contains}
   :nth    {:up     :nth-ancestor
            :down   :nth-contains}})

(def any? (constantly true))

(defn expand-special [ele]
  (if (keyword? ele)
    (or (if (= :* ele) {:type :multi})
        (if-let [step (try (Integer/parseInt (name ele))
                           (catch java.lang.NumberFormatException e))]
          (if (> step 0)
            {:type :nth :step step}))
        (throw (ex-info "Not a valid keyword (either :* or :<natural number>)" {:value ele})))))

(defn expand-path
  ([path] (expand-path path []))
  ([[x y & xs :as more] out]
   (if-not (empty? more)
     (let [xmap  (expand-special x)
           xmeta (if (instance? clojure.lang.IObj x)
                   (meta (classify/normalise-meta x)))
           ymap  (expand-special y)
           ymeta (if (instance? clojure.lang.IObj y)
                   (meta (classify/normalise-meta y)))]
       
       (cond (and (nil? xmap) (= 1 (count more)))
             (conj out (merge {:type :step :element x} xmeta))
             
             (nil? xmap)
             (recur (cons y xs)
                    (conj out (merge {:type :step :element x} xmeta)))
             
             (and xmap ymap)    
             (recur (cons y xs)
                    (conj out (assoc xmap :element '_)))
             
             (and xmap (= 1 (count more)))
             (conj out (assoc xmap :element '_))
             
             :else
             (recur xs
                    (conj out (merge (assoc xmap :element y) ymeta)))))
     out)))

(defn compile-section-base [section]
  (let [{:keys [element] evaluate? :%} section]
    (cond evaluate?
          (compile-section-base (-> section
                                    (assoc :element (eval element))
                                    (dissoc :%)))

          (= '_ element)    {:is any?}
          (fn? element)     {:is element}
          (map? element)    (walk/postwalk
                             (fn [ele]
                               (cond (:% (meta ele))
                                     (eval (with-meta ele
                                             (-> (meta ele)
                                                 (dissoc :%))))
                                     :else ele))
                             element)
          (list? element)   {:pattern element}
          (symbol? element) {:form element}
          :else {:is element})))

(defn compile-section [direction prev {:keys [type step] optional? :? :as section}]
  (let [base (-> section
                 compile-section-base)
        dkey (get-in moves [type direction])
        current (merge base prev)
        current (if optional?
                  {:or #{current (merge {:is any?} prev)}}
                  current)]
    (if (= type :nth)
      {dkey [step current]}
      {dkey current})))

(defn compile-submap [direction sections]
  (reduce (fn [i section]
            (compile-section direction i section))
          nil sections))

(defn compile-map [{:keys [modifiers] :as mpaths}]
  (let [msections
        (reduce-kv (fn [m k path]
                     (assoc m k (reverse (expand-path path))))
                   {}
                   (dissoc mpaths :modifiers))
        
        ;; adjust the cursor if it is between two symbols
        [curr msections] (cond (and (= :vertical (:direction modifiers))
                                    (last down))
                               [(last down) (update-in msections [:down] butlast)]

                               (and (= :horizontal (:direction modifiers))
                                    (last left))
                               [(last left) (update-in msections [:left] butlast)]
                               
                               :else [{:element '_} msections])]
    (->> msections
         (map (fn [[k v]] (compile-submap k v)))
         (apply merge)
         (merge (-> curr
                    (compile-section-base))))))
