(ns jai.walk.position
  (:require [rewrite-clj.zip :as source]
            [clojure.zip :as pattern]
            [jai.common :refer [any none] :as common]
            [clojure.set :as set]))

(defn pattern-zip
  [root]
  (pattern/zipper #(or (seq? %) (vector? %))
                  identity
                  (fn [node children] (with-meta children (meta node)))
                  root))

(defn fit-sexpr
  ([sexpr pnode] (fit-sexpr sexpr pnode false))
  ([sexpr pnode exact?]
   (let [pnode (cond (= '_ pnode) any
                     :else pnode)]
     
     (cond exact?
           (= pnode sexpr)

           (or (fn? pnode) (var? pnode))
           (try (pnode sexpr)
                (catch Throwable t false))

           (map? pnode)
           (->> pnode
                (map (fn [[k v]]
                       (fit-sexpr (get sexpr k) v exact?)))
                (every? true?))
           
           (set? pnode)
           (or (pnode sexpr)
               (and (set? sexpr)
                    (empty? (set/difference pnode sexpr))))
           
           :else
           (= pnode sexpr)))))

(defn walk-horizontal-many [{:keys [level backtrack current] :as state}]
  (let [moves (->> (:source current)
                   (iterate source/right)
                   (take-while identity))
        num   (count moves)]
    (merge state {:current  {:source (last moves)
                             :pattern (pattern/rightmost (:pattern current))}
                  :backtrack (concat (repeat (dec num) :left) backtrack)})))

(declare walk-down walk-horizontal)

(defn walk-remove [{:keys [level backtrack current] :as state}]
  (let [leftmost  (source/leftmost? (:source current))
        rightmost (source/rightmost? (:source current))
        sloc      (cond (and leftmost rightmost)
                        (throw (ex-info "Removal of only element not supported"
                                        {:source (source/node (:source current))}))

                        leftmost
                        (-> current :source source/remove source/down)

                        :else
                        (-> current :source source/remove))]
    (merge state {:current  {:source   (source/right sloc)
                             :pattern  (pattern/right (:pattern current))}
                  :previous {:source sloc
                             :pattern (:pattern current)}})))

(defn walk-horizontal [{:keys [level backtrack current] :as state}]
  (let [sexpr (-> current :source source/sexpr)
        pnode (-> current :pattern pattern/node)
        pnode (common/expand-meta pnode)
        {optional? :?
         insert?   :+
         exact?    :&
         remove?   :-
         evalute?  :%} (meta pnode)
        pnode (if evalute? (eval pnode) pnode)]
    (cond insert?
          (merge state {:current {:source  (source/insert-left (:source current) pnode)
                                  :pattern (pattern/right (:pattern current))}
                        :previous current
                        :backtrack (cons :left backtrack)})

          (= '& pnode)
          (walk-horizontal-many (merge state {:previous current}))

          (= '| pnode)
          (merge state {:current {:source  (:source current)
                                  :pattern (pattern/right (:pattern current))}
                        :previous  current
                        :backtrack ()
                        :cursor true})
          
          (and (or (list? pnode) (vector? pnode))
               (not (empty? pnode)))
          (let [{:keys [current backtrack] :as result}
                (walk-down (merge state {:current {:source  (source/down  (:source current))
                                                   :pattern (pattern/down (:pattern current))}
                                         :previous current
                                         :backtrack (cons :up backtrack)}))]
            (merge result {:current {:source  (source/right (:source current))
                                     :pattern (pattern/right (:pattern current))}
                           :previous current
                           :level level}))
          
          :else
          (let [fit? (fit-sexpr sexpr pnode exact?)]
            (cond fit?
                  (if remove?
                    (walk-remove state)
                    (merge state {:current {:source  (source/right  (:source current))
                                            :pattern (pattern/right (:pattern current))}
                                  :previous current
                                  :backtrack (cons :left backtrack)}))

                  optional?
                  (walk-horizontal
                   (merge state {:current {:source  (:source current)
                                           :pattern (pattern/right (:pattern current))}
                                 :previous current}))

                  :else
                  (throw (ex-info "Match not found: " {:pattern pnode :source sexpr})))))))

(defn walk-up [{:keys [level previous current backtrack] :as state}]
  (cond (zero? level)
        state
        
        :else
        (walk-horizontal (merge state {:current {:source  (source/right  (:source current))
                                                 :pattern (pattern/right (:pattern current))}
                                       :previous current
                                       :backtrack (cons :left backtrack)}))))

(defn walk-down
  [state]
  (let [{:keys [level previous current backtrack] :as nstate} (walk-horizontal state)]
    (cond (-> current :source nil?)
          (walk-up (merge nstate {:current {:source  (source/up (:source previous))
                                            :pattern (pattern/up (:pattern previous))}
                                  :level (dec level)
                                  :backtrack (concat '(:down :rightmost) backtrack)}))
          
          :else
          (walk-down nstate))))

(defn walk [source pattern]
  (let [result (walk-down {:current {:source  (source/down source)
                                     :pattern (pattern/down (pattern-zip pattern))}
                           :level 1
                           :backtrack '(:up)})]
    (if (:cursor result)
      (let [fns    {:up        source/up
                    :down      source/down
                    :rightmost source/rightmost
                    :left      #(if-let [loc (source/left %)]
                                  loc %)}]
        (reduce (fn [zloc instruction]
                  ((get fns instruction) zloc))
                (-> result :current :source)
                (let [bt (:backtrack result)]
                  (if (= :left (last bt)) (butlast bt) bt))))
      (-> result :current :source))))
