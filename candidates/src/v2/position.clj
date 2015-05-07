(ns jai.position
  (:require [rewrite-clj.zip :as source]
            [clojure.zip :as pattern]
            [jai.common :refer [any none] :as common]
            [clojure.set :as set]))

(defn potential-deletions [pnode]
  (count (take-while #(:- (meta %)) pnode)))

(defn pattern-zip
  [root]
  (pattern/zipper #(or (seq? %) (vector? %))
                  identity
                  (fn [node children] (with-meta children (meta node)))
                  root))

(defn check-expression
  ([sexpr pnode] (check-expression sexpr pnode false))
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
                       (check-expression (get sexpr k) v exact?)))
                (every? true?))
           
           (set? pnode)
           (or (pnode sexpr)
               (and (set? sexpr)
                    (empty? (set/difference pnode sexpr))))
           
           :else
           (= pnode sexpr)))))

(defrecord Position [source pattern previous backtrack op]
  Object
  (toString [pos]
    (str "#pos" {:source (source/sexpr source)
                 :pattern (pattern/node pattern)
                 :backtrack backtrack})))

(defmethod print-method Position
  [v w]
  (.write w (str v)))

(defn wrap-meta [f]
  (fn [{:keys [source backtrack] :as state}]
    (if (not= :meta (source/tag source))
      (f state)
      (let [nstate (f (assoc state
                             :source (-> source source/down source/right)
                             :backtrack (concat '(:up) backtrack)))]
        (assoc nstate
               :source (-> (:source nstate) source/up)
               :backtrack (->> (:backtrack nstate) (concat '(:down :right))))))))

(defn wrap-previous [f]
  (fn [{:keys [source pattern] :as state}]
    (let [nstate (f state)]
      (assoc nstate :previous {:source source
                               :pattern pattern}))))

(defn wrap-move [f]
  (fn [state]
    (let [{:keys [source pattern] :as nstate} (f state)]
      )))

(defn traverse-level-many [{:keys [source pattern backtrack] :as state}]
  (let [moves (->> source
                   (iterate source/right)
                   (take-while identity))
        num   (count moves)]
    (assoc state
           :source  (last moves)
           :pattern (pattern/rightmost pattern)
           :backtrack (concat (repeat (dec num) :left) backtrack))))

(defn traverse-level [{:keys [source pattern op] :as state}]
  (let [sexpr (source/sexpr source)
        pnode (pattern/node pattern)
        {optional? :?
         insert?   :+
         exact?    :&
         remove?   :-
         evalute?  :%} (meta pnode)
        pnode (if evalute? (eval pnode) pnode)]
    (cond insert?
          (assoc state
                 :source  (source/left (source/insert-left source pnode))
                 :backtrack (cons :left (:backtrack state)))

          (= '| pnode)
          (assoc state
                 :source (or (source/left source) (source/leftmost source))
                 :backtrack ()
                 :cursor true)

          (= '& pnode)
          ((:level-many op) state)

          (and (or (list? pnode) (vector? pnode))
               (not (empty? pnode)))
          ((:form op) state)

          :else
          (let [fit? (check-expression sexpr pnode exact?)]
            (cond fit?
                  (if remove?
                    (throw (Exception. "REMOVE NOT IMPLEMENTED"))
                    state)

                  optional?
                  (throw (Exception. "OPTIONAL NOT IMPLEMENTED"))
                  
                  :else
                  (throw (ex-info "Match not found: " {:pattern pnode :source sexpr})))))))

(defn traverse-form [{:keys [source pattern op] :as state}]
  (let [sexpr (source/sexpr source)
        pnode (pattern/node pattern)]
    (cond (empty? sexpr)
          (cond (empty? pnode) state
                :else ((:empty-insert op) state))

          (< (count sexpr) (potential-deletions pnode))
          ((:empty-delete op) state)

          :else
          ((:level op) (assoc state
                              :source    (source/down source)
                              :pattern   (pattern/down pattern)
                              :backtrack (cons :up (:backtrack state)))))))

(defn traverse [source template]
  (let [pattern (pattern-zip template)
        op    {:form         traverse-form
               :level        (-> traverse-level
                                 wrap-meta
                                 wrap-previous
                                 wrap-move)
               :level-many   traverse-level-many
               :empty-insert identity
               :empty-delete identity}]
    ((:form op) (map->Position {:source source
                                :pattern pattern
                                :backtrack ()
                                :op op}))))
