(ns jai.query.path-test
  (:use midje.sweet)
  (:require [jai.query.path :refer :all]
            [jai.query.path.classify :as classify]))

{:refer jai.query.path/expand-special :added "0.1"}
(fact "expanding the special keywords before symbols"
  (expand-special 'a) => nil
  (expand-special :*) => {:type :multi}
  (expand-special :3) => {:type :nth, :step 3}
  (expand-special :0) => throws)

{:refer jai.query.path/expand-path :added "0.1"}
(fact "expanding the vector to a better representation"
  (expand-path '[:* if :3 ^:? defn])
  => '[{:element if, :type :multi}
       {:element defn, :type :nth, :step 3 :? true}]
  
  (expand-path '[:* :2 defn])
  => '[{:type :multi :element _}
       {:type :nth :element defn :step 2}]

  (expand-path '[:* if :3])
  => '[{:type :multi :element if}
       {:type :nth :element _ :step 3}])


{:refer jai.query.path/compile-section-base :added "0.1"}
(fact "compiling the section element to its map representation"
  (compile-section-base '{:element +}) => {:form '+}
  (compile-section-base '{:element _}) => {:is any?}
  (compile-section-base '{:element vector? :% true}) => {:is vector?}
  (compile-section-base '{:element (+ 1 2 3)}) => {:pattern '(+ 1 2 3)})

{:refer jai.query.path/compile-section :added "0.1"}
(fact "compiling section to its map representation based on context"
  (compile-section :up nil '{:type :multi :element if})
  => '{:ancestor {:form if}}

  (compile-section :left {:is vector?} '{:type :multi :element if})
  => {:left-of {:form 'if :is vector?}}

  (compile-section :down {:is vector?} '{:type :nth :step 2 :element if})
  => {:nth-level [2 {:is vector?, :form 'if}]})

{:refer jai.query.path/compile-submap :added "0.1"}
(fact "part of the map compilation"
  (compile-submap :up (expand-path '[if :1 defn]))
  => '{:nth-ancestor [1 {:parent {:form if}, :form defn}]}

  (compile-submap :down (expand-path '[if :* defn]))
  => '{:contains {:child {:form if}, :form defn}})

{:refer jai.query.path/compile-map :added "0.1"}
(fact "testing from path to map"
  (-> '[defn :1 if [_ | ^:%? vector?] + -]
      (classify/classify)
      (compile-map))
  => {:parent {:nth-ancestor [1 {:form 'defn}], :form 'if},
      :child {:child {:form '-}, :form '+},
      :left {:is any?},
      :right {:or #{{:is vector?}
                    {:is any?}}}}

  (-> '[if [_ | ^:$ _]]
      (classify/classify)
      (compile-map)))

