(ns jai.query.traverse
  (:require [rewrite-clj.zip :as source]
            [clojure.zip :as pattern]
            [jai.match.pattern :refer [pattern-matches]]
            [jai.match.optional :as optional]
            [jai.common :as common]
            [clojure.walk :as walk]))

(defrecord Position [source pattern op]
  Object
  (toString [pos]
    (str "#pos" {:source (source/sexpr source)
                 :pattern (pattern/node pattern)})))

(defmethod print-method Position
  [v w]
  (.write w (str v)))

(defn pattern-zip
  [root]
  (pattern/zipper #(or (seq? %) (vector? %))
              identity
              (fn [node children] (with-meta children (meta node)))
              root))

(defn wrap-meta [f]
  (fn [{:keys [source level] :as pos}]
    (if (not= :meta (source/tag source))
      (f pos)
      (let [ppos   (if level (update-in pos [:level] inc) pos)
            npos   (f (assoc ppos :source (-> source source/down source/right)))]
        (if (:end npos)
          npos
          (assoc npos
                 :source (-> (:source npos) source/up)
                 :level level))))))

(defn wrap-delete-next [f]
  (fn [{:keys [source pattern next] :as pos}]
    (if next
      (if-let [nsource (source/right source)]
        (f (-> pos
               (assoc :source nsource :pattern (pattern/right pattern))
               (dissoc :next)))
        (-> pos
            (assoc :source (source/up source) :pattern (pattern/up pattern))
            (dissoc :next)))
      (f pos))))

(defn traverse-delete-form [{:keys [source pattern op] :as pos}]
  (let [sexpr (source/sexpr source)
        pnode (pattern/node pattern)]
    (if (empty? pnode)
      pos
      ((:delete-level op) (assoc pos
                                 :source  (source/down source)
                                 :pattern (pattern/down pattern))))))

(defn traverse-delete-node [{:keys [source pattern op] :as pos}]
  (cond (and (source/leftmost? source)
             (source/rightmost? source))
        (assoc pos
               :source (source/remove source)
               :pattern (pattern/up pattern))
        
        :else
        ((:delete-level op)
         (assoc pos
                :source (source/remove source)
                :pattern pattern
                :next true))))

(defn traverse-delete-level [{:keys [source pattern op] :as pos}]
  (let [pnode (pattern/node pattern)
        sexpr (source/sexpr source)
        delete? (-> pnode meta :-)]
    (cond (= '& pnode)
          ((:delete-level op) (assoc pos
                                     :source  (source/rightmost source)
                                     :pattern (pattern/rightmost pattern)
                                     :next true))
          
          delete?
          ((:delete-node op) pos)

          (and (or (list? pnode) (vector? pnode))
               (not (empty? pnode)))
          (-> pos
              ((:delete-form op))
              (assoc :next true)
              ((:delete-level op)))

          :else
          ((:delete-level op) (assoc pos :next true)))))


(defn prep-insert-pattern [pattern]
  (let [pnode (pattern/node pattern)
        {evaluate? :%} (meta pnode)]
    (if evaluate? (eval pnode) (with-meta pnode nil))))

(defn wrap-insert-next [f]
  (fn [{:keys [source pattern next] :as pos}]
    (if-not next
      (f pos)
      (let [nsource (source/right source)
            npattern (pattern/right pattern)]
        (cond (and nsource npattern)
              (f (-> pos
                     (assoc :source nsource
                            :pattern npattern)
                     (dissoc :next)))

              (and npattern (not= '& (pattern/node npattern)))
              (let [inserts (->> (iterate pattern/right npattern)
                                 (take-while identity)
                                 (map prep-insert-pattern))
                    nsource (reduce source/insert-right source (reverse inserts))]
                (-> pos
                    (assoc :source  (source/up nsource)
                           :pattern (pattern/up pattern))
                    (dissoc :next)))

              :else
              (-> pos
                  (assoc :source (source/up source) :pattern (pattern/up pattern))
                  (dissoc :next)))))))

(defn traverse-insert-form [{:keys [source pattern op] :as pos}]
  (let [sexpr (source/sexpr source)
        pnode (pattern/node pattern)]
    (cond (empty? pnode)
          pos

          (empty? sexpr)
          (assoc pos :source (reduce source/append-child source (reverse pnode)))

          :else
          ((:insert-level op) (assoc pos
                                     :source (source/down source)
                                     :pattern (pattern/down pattern))))))

(defn traverse-insert-node [{:keys [source pattern op] :as pos}]
  ((:insert-level op)
   (let [val (prep-insert-pattern pattern)]
     (assoc pos
            :source (source/left (source/insert-left source val))
            :next true))))

(defn traverse-insert-level [{:keys [source pattern op] :as pos}]
  (let [pnode (pattern/node pattern)
        sexpr (source/sexpr source)
        insert? (-> pnode meta :+)]
    (cond (= '& pnode)
          ((:insert-level op) (assoc pos
                                     :source  (source/rightmost source)
                                     :pattern (pattern/rightmost pattern)
                                     :next true))
          
          insert?
          ((:insert-node op) pos)

          (and (or (list? pnode) (vector? pnode))
               (not (empty? pnode)))
          (-> pos
              ((:insert-form op))
              (assoc :next true)
              ((:insert-level op)))

          :else
          ((:insert-level op) (assoc pos :next true)))))

(defn wrap-cursor-next [f]
  (fn [{:keys [source pattern next end] :as pos}]
    (cond end pos
          
          next
          (let [nsource (source/right source)
                npattern (pattern/right pattern)]
            (cond (and nsource npattern)
                  (f (-> pos
                         (assoc :source nsource :pattern npattern)
                         (dissoc :next)))

                  npattern
                  (f (-> pos
                         (assoc :source source :pattern npattern)
                         (dissoc :next)))

                  (nil? nsource)
                  (-> pos
                      (assoc :source (source/up source) :pattern (pattern/up pattern))
                      (update-in [:level] dec)
                      (dissoc :next))))
          :else (f pos))))

(defn traverse-cursor-form [{:keys [source pattern op] :as pos}]
  (let [sexpr (source/sexpr source)
        pnode (pattern/node pattern)
        pos   (update-in pos [:level] inc)]
    (cond (empty? pnode)
          pos

          :else
          ((:cursor-level op) (assoc pos
                                     :source (source/down source)
                                     :pattern (pattern/down pattern))))))

(defn traverse-cursor-level [{:keys [source pattern op] :as pos}]
  (let [sexpr (source/sexpr source)
        pnode (pattern/node pattern)]
    (cond (= '| pnode)
          ((:cursor-level op) (assoc pos :end true))
          
          (= '& pnode)
          ((:cursor-level op) (assoc pos
                                     :source  (source/rightmost source)
                                     :pattern (pattern/rightmost pattern)
                                     :next true))
          
          
          (and (or (list? pnode) (vector? pnode))
               (not (empty? pnode)))
          (-> pos
              ((:cursor-form op))
              (assoc :next true)
              ((:cursor-level op)))

          :else
          ((:cursor-level op) (assoc pos :next true)))))

(defn count-elements [pattern]
  (let [sum (atom 0)]
    (walk/postwalk (fn [x] (swap! sum inc))
                  pattern)
    @sum))

(defn traverse [source pattern]
  (let [pseq    (optional/pattern-seq pattern)
        lookup  (->> pseq
                     (map (juxt common/prepare-deletion
                                identity))
                     (into {}))
        p-dels   (->> (source/sexpr source)
                      ((pattern-matches (common/prepare-deletion pattern))))
        p-del   (case  (count p-dels)
                  0 (throw (ex-info "Needs to have a match."
                                    {:matches p-dels
                                     :source (source/sexpr source)
                                     :pattern pattern}))
                  1 (first p-dels)
                  (->> p-dels
                       (sort-by count-elements)
                       (last)))
        p-match (get lookup p-del)
        p-ins   (common/prepare-insertion p-match)
        op-del  {:delete-form  (wrap-meta traverse-delete-form)
                 :delete-level (wrap-delete-next traverse-delete-level)
                 :delete-node  traverse-delete-node}
        
        del-pos (-> (map->Position {:source  source
                                    :pattern (pattern-zip p-del)
                                    :op op-del})
                    ((:delete-form op-del)))
        
        op-ins  {:insert-form  (wrap-meta traverse-insert-form)
                 :insert-level (wrap-insert-next traverse-insert-level)
                 :insert-node  traverse-insert-node}
        ins-pos (-> del-pos
                    (assoc :pattern (pattern-zip p-ins)
                           :op op-ins)
                    ((:insert-form op-ins)))
        p-cursor (common/remove-items common/deletion? p-match)]
    (if (= p-cursor p-ins)
      ins-pos
      (let [op-cursor {:cursor-form  (wrap-meta traverse-cursor-form)
                       :cursor-level (wrap-cursor-next traverse-cursor-level)}
            cursor-pos (-> ins-pos
                           (assoc :pattern (pattern-zip p-cursor)
                                  :op op-cursor
                                  :level 0)
                           ((:cursor-form op-cursor)))]
        cursor-pos))))
