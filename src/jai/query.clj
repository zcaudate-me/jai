(ns jai.query
  (:require [jai.common :as common]
            [jai.match :as match]
            [jai.query.traverse :as traverse]
            [jai.query.walk :as query]
            [clojure.walk :as walk]))

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
          [(dec (count selectors)) :form (last selectors)]
          [nil :cursor])
      1 (let [max      (dec (count selectors))
              [i type :as candidate] (first candidates)
              _ (case type
                  :form   (if (not= i max)
                            (throw (Exception. "Form should be in the last position of the selectors")))
                  :cursor (if (= i max)
                            (throw (Exception. "Cursor cannot be in the last position of the selectors"))))]
          candidate)
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


(defn process-special [ele]
  (if (keyword? ele)
    (or (if (= :* ele) {:type :multi})
        (if-let [step (try (Integer/parseInt (name ele))
                           (catch java.lang.NumberFormatException e))]
          (if (> step 0)
            {:type :nth :step step}))
        (throw (ex-info "Not a valid keyword (either :* or :<natural number>)" {:value ele})))))

(defn process-path
  ([path] (process-path path []))
  ([[x y & xs :as more] out]
   (if-not (empty? more)
     (let [xmap  (process-special x)
           xmeta (meta x)
           ymap  (process-special y)
           ymeta (meta y)]
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

          (= '_ element)    {:is common/any}
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

(def moves
  {:step   {:up     :parent
            :down   :child}
   :multi  {:up     :ancestor
            :down   :contains}
   :nth    {:up     :nth-ancestor
            :down   :nth-contains}})

(defn compile-section [direction prev {:keys [type step] optional? :? :as section}]
  (let [base (-> section
                 compile-section-base)
        dkey (get-in moves [type direction])
        current (merge base prev)
        current (if optional?
                  {:or #{current (merge {:is common/any} prev)}}
                  current)]
    (if (= type :nth)
      {dkey [step current]}
      {dkey current})))

(defn compile-submap [direction sections]
  (reduce (fn [i section]
            (compile-section direction i section))
          nil sections))

(defn match [selectors]
  (let [selectors  (expand-all-metas selectors)
        [cidx ctype cform :as cursor]     (cursor-info selectors)
        qselectors (mapv (fn [ele]
                           (if (list? ele)
                             (common/prepare-query ele) ele))
                         selectors)
        {:keys [up down]}   (split-path qselectors cursor)
        up (process-path up)
        [curr & down] (process-path down)
        match-map (merge (compile-section-base curr)
                         (compile-submap :up up)
                         (compile-submap :down down))
        match-fn (match/compile-matcher match-map)]
    [match-fn cursor]))

(defn select [zloc selectors]
  (let [[match-fn [cidx ctype cform]] (match selectors)]
      (let [atm  (atom [])]
        (query/matchwalk zloc
                         [match-fn]
                         (fn [zloc]
                           (if (= :form ctype)
                             (swap! atm conj (traverse/traverse zloc cform))
                             (swap! atm conj zloc))
                           zloc))
    @atm)))

(defmacro $ [context selectors]
  `(map z/sexpr (select ~context (quote ~selectors))))


(comment
  (require '[rewrite-clj.zip :as z])

  
  (select (z/of-string "(defn hello)") '[(defn ^:?- _ | & _)])
  ($ (z/of-string "(defn hello)") [(defn | & _)])

  (select (z/of-file ))

  (cursor-info '[(defn ^:?& _ | & _)])

  (cursor-info (expand-all-metas '[(defn ^:?& _ | & _)]))

  (potential-cursors (expand-all-metas '[(defn & _)]))

  ($ nil [(defn _ | & _)])

  (set! *print-meta* (not *print-meta*))
  )
