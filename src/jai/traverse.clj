(ns jai.traverse
  (:require [rewrite-clj.zip :as source]
            [clojure.zip :as zip]
            [jai.match.pattern :as pattern]
            [jai.match.optional :as optional]
            [jai.match.common :as common]))

(defrecord Position [source pattern op]
  Object
  (toString [pos]
    (str "#pos" {:source (source/sexpr source)
                 :pattern (zip/node pattern)})))

(defmethod print-method Position
  [v w]
  (.write w (str v)))

(defn pattern-zip
  [root]
  (zip/zipper #(or (seq? %) (vector? %))
              identity
              (fn [node children] (with-meta children (meta node)))
              root))

(defn cursor? [ele] (= '| ele))

(defn insertion? [ele] (-> ele meta :+))

(defn deletion? [ele] (-> ele meta :-))

(defn prepare-deletion [pattern]
  (-> pattern
      (common/remove-items cursor?)
      (common/remove-items insertion?)))

(defn prepare-insertion [pattern]
  (-> pattern
      (common/remove-items cursor?)
      (common/remove-items deletion?)))

(defn wrap-meta [f]
  (fn [{:keys [source] :as pos}]
    (if (not= :meta (source/tag source))
      (f pos)
      (let [npos (f (assoc pos :source (-> source source/down source/right)))]
        (assoc npos :source (-> (:source npos) source/up))))))

(defn wrap-delete-next [f]
  (fn [{:keys [source pattern next] :as pos}]
    (if next
      (if-let [nsource (source/right source)]
        (f (-> pos
               (assoc :source nsource :pattern (zip/right pattern))
               (dissoc :next)))
        (-> pos
            (assoc :source (source/up source) :pattern (zip/up pattern))
            (dissoc :next)))
      (f pos))))

(defn traverse-delete-form [{:keys [source pattern op] :as pos}]
  (let [sexpr (source/sexpr source)
        pnode (zip/node pattern)]
    (if (empty? pnode)
      pos
      ((:delete-level op) (assoc pos
                                 :source  (source/down source)
                                 :pattern (zip/down pattern))))))

(defn traverse-delete-node [{:keys [source pattern op] :as pos}]
  (cond (and (source/leftmost? source)
             (source/rightmost? source))
        (assoc pos
               :source (source/remove source)
               :pattern (zip/up pattern))
        
        :else
        ((:delete-level op)
         (assoc pos
                :source (source/remove source)
                :pattern pattern
                :next true))))

(defn traverse-delete-level [{:keys [source pattern op] :as pos}]
  (let [pnode (zip/node pattern)
        sexpr (source/sexpr source)
        delete? (-> pnode meta :-)]
    (cond (= '& pnode)
          ((:delete-level op) (assoc pos
                                     :source  (source/rightmost source)
                                     :pattern (zip/rightmost pattern)
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

(defn wrap-insert-next [f]
  (fn [{:keys [source pattern next] :as pos}]
    (if-not next
      (f pos)
      (let [nsource (source/right source)
            npattern (zip/right pattern)]
        (cond (and nsource npattern)
              (f (-> pos
                     (assoc :source nsource
                            :pattern npattern)
                     (dissoc :next)))

              npattern
              (let [inserts (->> (iterate zip/right npattern)
                                 (take-while identity)
                                 (map zip/node))
                    nsource (reduce source/insert-right source (reverse inserts))]
                (-> pos
                    (assoc :source  (source/up nsource)
                           :pattern (zip/up pattern))
                    (dissoc :next)))

              :else
              (-> pos
                  (assoc :source (source/up source) :pattern (zip/up pattern))
                  (dissoc :next)))))))

(defn traverse-insert-form [{:keys [source pattern op] :as pos}]
  (let [sexpr (source/sexpr source)
        pnode (zip/node pattern)]
    (cond (empty? pnode)
          pos

          (empty? sexpr)
          (assoc pos :source (reduce source/append-child source (reverse pnode)))

          :else
          ((:insert-level op) (assoc pos
                                     :source (source/down source)
                                     :pattern (zip/down pattern))))))

(defn traverse-insert-node [{:keys [source pattern op] :as pos}]
  ((:insert-level op)
         (assoc pos
                :source (source/left (source/insert-left source))
                :next true)))

(defn traverse-insert-level [{:keys [source pattern op] :as pos}]
  (let [pnode (zip/node pattern)
        sexpr (source/sexpr source)
        insert? (-> pnode meta :+)]
    (cond (= '& pnode)
          ((:insert-level op) (assoc pos
                                     :source  (source/rightmost source)
                                     :pattern (zip/rightmost pattern)
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
                npattern (zip/right pattern)]
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
                      (assoc :source (source/up source) :pattern (zip/up pattern))
                      (dissoc :next))))
          :else (f pos))))

(defn traverse-cursor-form [{:keys [source pattern op] :as pos}]
  (let [sexpr (source/sexpr source)
        pnode (zip/node pattern)]
    (cond (empty? pnode)
          pos

          :else
          ((:cursor-level op) (assoc pos
                                     :source (source/down source)
                                     :pattern (zip/down pattern))))))

(defn traverse-cursor-level [{:keys [source pattern op] :as pos}]
  (let [sexpr (source/sexpr source)
        pnode (zip/node pattern)]
    (cond (= '| pnode)
          (assoc pos :end true)
          
          (= '& pnode)
          ((:cursor-level op) (assoc pos
                                     :source  (source/rightmost source)
                                     :pattern (zip/rightmost pattern)
                                     :next true))
          
          
          (and (or (list? pnode) (vector? pnode))
               (not (empty? pnode)))
          (-> pos
              ((:cursor-form op))
              (assoc :next true)
              ((:cursor-level op)))

          :else
          ((:cursor-level op) (assoc pos :next true)))))

(defn traverse [source pattern]
  (let [pseq    (optional/pattern-seq pattern)
        lookup  (->> pseq
                     (map (juxt prepare-deletion
                                identity))
                     (into {}))
        p-del   (->> (source/sexpr source)
                     ((pattern/pattern-matches (prepare-deletion pattern)))
                    (first))
        p-match (get lookup p-del)
        
        p-ins   (prepare-insertion p-match)
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
        p-cursor (common/remove-items p-match deletion?)]
    (if (= p-cursor p-ins)
      (:source ins-pos)
      (let [op-cursor {:cursor-form  (wrap-meta traverse-cursor-form)
                       :cursor-level (wrap-cursor-next traverse-cursor-level)}
            cursor-pos (-> ins-pos
                           (assoc :pattern (pattern-zip p-cursor)
                                  :op op-cursor)
                           ((:cursor-form op-cursor)))]
        (:source cursor-pos)))))
