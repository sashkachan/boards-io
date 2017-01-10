(defproject boards-io "0.1.0"
  :description "Boards and columns"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.6.1"
  :jvm-opts ^:replace ["-Xms512m" "-Xmx512m" "-server"]
  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.clojure/core.async "0.2.385"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/tools.logging   "0.3.1"]
                 [com.datomic/datomic-free    "0.9.5394" :exclusions [com.google.guava/guava #_org.slf4j/log4j-over-slf4j #_ org.slf4j/slf4j-nop]]
                 [com.stuartsierra/component  "0.3.1"]
                 [bidi "2.0.11"]
                 [com.cognitect/transit-cljs  "0.8.239"]
                 [com.cognitect/transit-clj   "0.8.288"]
                 [ring "1.5.0"]
                 [org.omcljs/om "1.0.0-alpha47"]
                 [figwheel-sidecar "0.5.9-SNAPSHOT"]
                 [com.cemerick/piggieback "0.2.1"]
                 [org.danielsz/system         "0.3.2-SNAPSHOT"]
                 [hiccup "1.0.5"]]
  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-exec "0.3.6"]]
  :main boards-io.core
  :cljsbuild {:builds [{:id "test"
                        :source-paths ["src/clj" "src/cljs" "src/test"]
                        :compiler {
                                   :output-to "script/tests.simple.js"
                                   :output-dir "script/out"
                                   :source-map "script/tests.simple.js.map"
                                   :output-wrapper false
                                   :optimizations :simple}}]}
  :source-paths ["src/clj" "src/cljs" "src/dev"]
  :hooks [leiningen.cljsbuild]
  :clean-targets ^{:protect false} ["resources/public/js" "target"])

