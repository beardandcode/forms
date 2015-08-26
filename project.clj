(defproject com.beardandcode/forms "0.1.0-SNAPSHOT"
  :description "A library to generate and validate HTML forms based on JSON Schema"
  :url "https://github.com/beardandcode/forms"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}

  :min-lein-version "2.0.0"
  
  :plugins [[lein-ancient "0.6.7"]
            [jonase/eastwood "0.2.1"]
            [lein-bikeshed "0.2.0"]
            [lein-kibit "0.1.2"]]
  
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [cheshire "5.5.0"]
                 [com.github.fge/json-schema-validator "2.2.6"]]

  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :java-source-paths ["src/java"]
  :resource-paths ["src"]

  :javac-options ["-target" "1.8" "-source" "1.8"]

  :aliases {"checkall" ["do" ["check"] ["kibit"] ["eastwood"] ["bikeshed"]]}
  
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.10"]
                                  [leiningen #=(leiningen.core.main/leiningen-version)]
                                  [im.chit/vinyasa "0.3.4"]

                                  ;; for tests
                                  [hickory "0.5.4"]

                                  ;; for test webapp
                                  [ring/ring-jetty-adapter "1.4.0"]
                                  [compojure "1.4.0"]
                                  [hiccup "1.0.5"]]
                   :source-paths ["dev"]
                   :resource-paths ["test"]}})
