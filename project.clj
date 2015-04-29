(defproject im.chit/juy "0.1.0"
  :description "Manipulate source code like the DOM"
  :url "http://github.com/zcaudate/juy"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-midje-doc "0.0.24"]]}}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.match "0.2.2"]
                 [rewrite-clj "0.4.12"]
                 [im.chit/hara.common "2.1.11"]])
