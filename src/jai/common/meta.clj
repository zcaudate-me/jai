(ns jai.common.meta
  (:require [rewrite-clj.zip :as z]
            [clojure.zip :as zip]))


{:match  '(defn _ ^:%? string? ^:%? map? ^:% vector? & _)
 :cursor '(defn _ ^:%? string? ^:%? map? | ^:% vector? & _)
 :delete '(defn _ ^:%? string? ^:%? map?)}

(comment

  (defn test-matcher [zloc matcher]
    (let [value (z/sexpr zloc)
          {evalute? :%} (meta matcher)
          matcher (cond (= '_ matcher) any?
                        evalute? (eval matcher) matcher)]
      (if (fn? matcher)
        (matcher value)
        (= matcher value))))

  (defn walk-form
    ([zloc matchers]
     (walk-form (z/down zloc) matchers 1))
    ([zloc [matcher & more :as matchers] level]
     (let [{optional? :?
            remove?   :-
            insert?   :+
            evalute?  :%} (meta matcher)
            fit? (test-matcher zloc matcher)]
       (cond (nil? zloc)
             (throw (ex-info "Walked too far"))

             (empty? matchers)
             (if (= 0 level) zloc (z/up zloc))

             (not fit?)
             (cond (not optional?)
                   (throw (ex-info "Form not meant to be walked" {:value (z/sexpr zloc)
                                                                  :matcher matcher}))

                   :else (recur zloc more level))

             remove?
             (recur (z/remove zloc) more level)

             insert?
             (recur (-> zloc (z/insert-right (if evalute? (eval matcher) matcher)) z/right)
                    more level)

             (and (= :list (z/tag zloc)) (list? matcher))
             (recur (walk-form (z/down zloc) matcher (inc level)) more level)
             
             :else
             (recur (z/right zloc) more level)))))

  (defn update-location [zloc matcher]
    )

  (comment

    (walk-form (z/of-string "(defn add [] (+ 1 1))")
               '(^:- defn ))

    (walk-form (z/of-string "(defn add [] (+ 1 1))")
               (defn ))
    
    '(defn _
       ^:%?- string? ^:%+ (str "function not around")
       ^:%?- map?    ^:+  {:added "0.1"}
       ^:% vector? | & _)

    '(defn _
       ^{:% true :? true :- true} string? ^{:% true :+ true} (str "function not around")
       ^{:% true :? true :- true} map?    ^{:+ true} {:added "0.1"}
       ^{:% true} vector?))

  )
