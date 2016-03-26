(defproject im.chit/jai "0.2.10"
  :description "Manipulate source code like the DOM"
  :url "http://github.com/zcaudate/jai"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :profiles {:dev {:dependencies [[midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]
                             [lein-midje-doc "0.0.24"]]}}
  :documentation {:files {"docs/index"
                           {:input "test/jai/readme_test.clj"
                            :title "jai"
                            :sub-title "Manipulate source code like the DOM"
                            :author "Chris Zheng"
                            :email  "z@caudate.me"
                            :tracking "UA-31320512-2"}}}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.match "0.2.2"]
                 [rewrite-clj "0.4.12"]
                 [im.chit/hara.common "2.2.17"]])
