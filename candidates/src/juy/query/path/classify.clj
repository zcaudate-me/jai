(ns gia.query.path.classify
  (:require [clojure.string :as string]))

(defn assoc-non-empty
  ([m k v]
   (if (empty? v) m (assoc m k v)))
  ([m k v & more]
   (apply assoc-non-empty (assoc-non-empty m k v) more)))

(defn find-index
  [path matches]
  (let [cursors (keep-indexed
                 (fn [i e] (some (fn [x]
                                   (if (cond (var? x) (@x e)
                                             :else (= x e))
                                     [i e]))
                                 matches)) path)]
    (case (count cursors)
      0 [-1 nil]
      1 (first cursors)
      (throw (ex-info (format "There should only be one of %s in the path:" matches) 
                      {:path path :cursors cursors :matches matches})))))

(defn normalise-meta [ele]
  (->> (meta ele)
       (keys)
       (map name)
       (apply str)
       (#(string/split % #""))
       (map keyword)
       (set)
       (#(-> %
             (zipmap (repeat true))
             (select-keys [:% :# :$ :?])))
       (with-meta ele)))

(defn- classify-horizontal [path]
  (let [[h-idx h-ele] (find-index path ['|])
        loc {:direction :horizontal}]
    (if (= h-idx -1)
      [(reverse path) nil loc]
      [(reverse (subvec path 0 h-idx))
       (subvec path (inc h-idx) (count path))
       (merge (meta (normalise-meta h-ele)) {:direction :horizontal})])))

(defn classify [path]
  (let [[v-idx v-ele] (find-index path ['| #'vector?])
        loc {:direction :vertical}
        [up down cur] (if (= v-idx -1)
                        [[] path nil]
                        [(reverse (subvec path 0 v-idx))
                         (subvec path (inc v-idx) (count path))
                         v-ele])
        [left right mods] (cond (nil? cur)
                                [[] [] loc]
                                
                                (= cur '|)
                                [[] [] (merge (meta (normalise-meta cur)) loc)]
          
                                (vector? cur)
                                (classify-horizontal cur))]
    (assoc-non-empty {} :up up :down down :left left :right right :modifiers mods)))
