(ns juy.paredit
  (:require [rewrite-clj.zip :as z]
            [rewrite-clj.node.whitespace :as ws])
  (:refer-clojure :exclude [slurp]))

(defn empty-type [type]
  (case type
    :list () :vector [] :map {} :set #{}
    () () [] [] {} {} #{} #{}))

(defn start [zloc]
  (cond (z/whitespace? zloc)
        (z/right zloc)

        :else zloc))

(defn open [zloc & [type & args]]
  (let [type   (or type :list)
        start-loc    (start zloc)
        bracket-loc  (-> start-loc
                         (z/insert-left (empty-type type))
                         (z/left))]
    (if (nil? args)
      (-> bracket-loc
          (z/insert-child (ws/whitespace-node " "))
          (z/down*))
      (-> (reduce (fn [zloc arg]
                    (z/insert-child zloc arg))
                  bracket-loc
                  (reverse args))
          (z/down)
          (z/rightmost)))))

(defn wrap
  ([zloc] (wrap zloc :list))
  ([zloc type]
   (let [start-loc  (start zloc)
         node       (z/node start-loc)]
     (-> start-loc
         (z/insert-left (empty-type type))
         (z/remove)
         (z/insert-child node)
         (z/down)))))

(defn- grab-nodes [zloc direction]
  (->> (iterate direction zloc)
       (take-while identity)
       (map z/node)))

(defn splice
  [zloc]
  (let [current     (z/node zloc)
        left        (grab-nodes (z/left zloc) z/left)
        right       (grab-nodes (z/right zloc) z/right)
        nloc        (-> (z/up zloc)
                        (z/replace current))
        nloc        (reduce (fn [zloc arg]
                              (z/insert-left zloc arg))
                            nloc
                            (reverse left))
        nloc        (reduce (fn [zloc arg]
                              (z/insert-right zloc arg))
                            nloc
                            (reverse right))]
    nloc))

(defn split
  [zloc]
  (let [parent   (-> zloc z/up z/tag)
        current  (z/node zloc)
        left     (grab-nodes (z/left zloc) z/left)
        right    (grab-nodes zloc z/right)
        nloc     (-> zloc z/up (z/replace ()))
        nloc     (reduce (fn [zloc arg]
                           (z/insert-child zloc arg))
                         nloc
                         (reverse left))
        nloc     (-> nloc
                     (z/insert-right ())
                     (z/right))
        nloc     (reduce (fn [zloc arg]
                           (z/insert-child zloc arg))
                         nloc
                         (reverse right))]
    nloc))

(defn join
  [zloc]
  (let [prev  (z/left zloc)
        sloc  (-> prev z/down)
        nodes (-> zloc z/down (grab-nodes z/right))]
  
    (-> (reduce (fn [zloc arg]
                  (z/insert-right zloc arg))
                (-> prev
                    z/right
                    z/remove
                    z/down
                    z/rightmost)
                (reverse nodes))
        
        
        
        )))

(defn slurp [])

(defn barf [])

