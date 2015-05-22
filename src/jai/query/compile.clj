(ns jai.query.compile
  (:require [jai.common :as common]
            [jai.match :as match]
            [jai.query.traverse :as traverse]
            [jai.query.walk :as query]
            [clojure.walk :as walk]))

(defn cursor-info
  "finds the information related to the cursor

  (cursor-info '[(defn ^:?& _ | & _)])
  => '[0 :form (defn _ | & _)]

  (cursor-info (expand-all-metas '[(defn ^:?& _ | & _)]))
  => '[0 :form (defn _ | & _)]

  (cursor-info '[defn if])
  => [nil :cursor]

  (cursor-info '[defn | if])
  => [1 :cursor]"
  {:added "0.2"}
  [selectors]
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

(defn expand-all-metas
  "converts the shorthand meta into a map-based meta
  (meta (expand-all-metas '^:%? sym?))
  => {:? true, :% true}

  (-> (expand-all-metas '(^:%+ + 1 2))
      first meta)
  => {:+ true, :% true}"
  {:added "0.2"}
  [selectors]
  (common/prewalk (fn [ele] (if (instance? clojure.lang.IObj ele)
                              (common/expand-meta ele)
                              ele))
                  selectors))

(defn split-path
  "splits the path into up and down
  (split-path '[defn | if try] [1 :cursor])
  => '{:up (defn), :down [if try]}

  (split-path '[defn if try] [nil :cursor])
  => '{:up [], :down [defn if try]}"
  {:added "0.2"}
  [selectors [idx ctype]]
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


(defn process-special
  "converts a keyword into a map
  (process-special :*) => {:type :multi}

  (process-special :1) => {:type :nth, :step 1}

  (process-special :5) => {:type :nth, :step 5}"
  {:added "0.2"}
  [ele]
  (if (keyword? ele)
    (or (if (= :* ele) {:type :multi})
        (if-let [step (try (Integer/parseInt (name ele))
                           (catch java.lang.NumberFormatException e))]
          (if (> step 0)
            {:type :nth :step step}))
        (throw (ex-info "Not a valid keyword (either :* or :<natural number>)" {:value ele})))))

(defn process-path
  "converts a path into more information
  (process-path '[defn if try])
  => '[{:type :step, :element defn}
       {:type :step, :element if}
       {:type :step, :element try}]

  (process-path '[defn :* try :3 if])
  => '[{:type :step, :element defn}
       {:element try, :type :multi}
       {:element if, :type :nth, :step 3}]"
  {:added "0.2"}
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

(defn compile-section-base
  "compiles an element section
  (compile-section-base '{:element defn})
  => '{:form defn}

  (compile-section-base '{:element (if & _)})
  => '{:pattern (if & _)}

  (compile-section-base '{:element _})
  => {:is jai.common/any}"
  {:added "0.2"}
  [section]
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

(defn compile-section
  "compile section
  (compile-section :up nil '{:element if, :type :nth, :step 3})
  => '{:nth-ancestor [3 {:form if}]}

  (compile-section :down nil '{:element if, :type :multi})
  => '{:contains {:form if}}"
  {:added "0.2"}
  [direction prev {:keys [type step] optional? :? :as section}]
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

(defn compile-submap
  "compile submap
  (compile-submap :down (process-path '[if try]))
  => '{:child {:child {:form if}, :form try}}

  (compile-submap :up (process-path '[defn if]))
  => '{:parent {:parent {:form defn}, :form if}}"
  {:added "0.2"}
  [direction sections]
  (reduce (fn [i section]
            (compile-section direction i section))
          nil sections))

(defn prepare
  "prepare
  (prepare '[defn if])
  => '[{:child {:form if}, :form defn} [nil :cursor]]
  
  (prepare '[defn | if])
  => '[{:parent {:form defn}, :form if} [1 :cursor]]"
  {:added "0.2"}
  [selectors]
  (let [selectors  (expand-all-metas selectors)
        [cidx ctype cform :as cursor]     (cursor-info selectors)
        qselectors (mapv (fn [ele]
                           (if (list? ele)
                             (common/prepare-deletion ele) ele))
                         selectors)
        {:keys [up down]}   (split-path qselectors cursor)
        up (process-path up)
        [curr & down] (process-path down)
        match-map (merge (compile-section-base curr)
                         (compile-submap :up (reverse up))
                         (compile-submap :down (reverse down)))]
    [match-map cursor]))
