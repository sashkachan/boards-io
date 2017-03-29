(defproject boards-io "0.1.0"
  :description "Boards and columns"
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.6.1"
  :jvm-opts ^:replace ["-Xms512m" "-Xmx512m" "-server"]
  :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                 :creds :gpg}}  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.229"]
                 [org.clojure/core.async "0.2.385"]
                 [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/tools.logging   "0.3.1"]
                 [com.datomic/datomic-pro "0.9.5394"]
                 [org.postgresql/postgresql "42.0.0"]
                 [bidi "2.0.11"]
                 [cljsjs/react-bootstrap "0.30.7-0" :exclusions [cljsjs.react]]
                 [com.stuartsierra/component  "0.3.1"]
                 [bidi "2.0.11"]
                 [com.cognitect/transit-cljs  "0.8.239"]
                 [com.cognitect/transit-clj   "0.8.300"]
                 [ring "1.5.0"]
                 [org.omcljs/om "1.0.0-alpha47"]
                 [ring-middleware-format "0.7.2"]
                 [org.danielsz/system         "0.3.1"]
                 [hiccup "1.0.5"]
                 [cheshire "5.7.0"]
                 [stuarth/clj-oauth2 "0.3.2"]
                 [cheshire "5.7.0"]
                 [clj-http "3.4.1"]

                 [ring-middleware-format "0.7.2"]]
  
  :plugins [[lein-cljsbuild "1.1.5"]]
  :main boards-io.core
  :profiles {:dev
             {:dependencies [[com.cemerick/piggieback "0.2.1"]
                             [figwheel-sidecar "0.5.9-SNAPSHOT"]]
              :repl-options {
                             :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
              :cljsbuild
              {:builds {:dev {:id "dev"
                              :source-paths ["src/clj" "src/cljs"]
                              :figwheel true
                              :compiler {:main "boards-io.core"
                                         :asset-path "/js",
                                         :recompile-dependents false
                                         :output-dir "resources/public/js/",
                                         :output-to "resources/public/js/main.js",
                                         :optimizations :none
                                         :source-map-timestamp true}}}}}
             
             :production
             {:cljsbuild
              {:builds {:production {:id "production"
                                     :source-paths ["src/clj" "src/cljs"]
                                     :compiler {:main "boards-io.core"
                                                :asset-path "/js",
                                                :optimizations :advanced
                                                :closure-defines {goog.DEBUG false}
                                                :recompile-dependents false
                                                :output-dir "resources/public/js/",
                                                :output-to "resources/public/js/main.js",
                                                :source-map-timestamp true}}}}}
             :test
             {:cljsbuild
              {:builds {:test {:id "test"
                               :source-paths ["src/clj" "src/cljs" "src/test"]
                               :compiler {
                                          :output-to "script/tests.simple.js"
                                          :output-dir "script/out"
                                          :source-map "script/tests.simple.js.map"
                                          :output-wrapper false
                                          :optimizations :simple}}}}}
             :cider
             {:dependencies [[cider/cider-nrepl "0.15.0-SNAPSHOT"]
                             [refactor-nrepl "2.3.0-SNAPSHOT"]
                             [org.clojure/tools.nrepl "0.2.12"]]
              :repl-options {:nrepl-middleware [refactor-nrepl.middleware/wrap-refactor
                                                cider.nrepl.middleware.apropos/wrap-apropos
                                                cider.nrepl.middleware.classpath/wrap-classpath
                                                cider.nrepl.middleware.complete/wrap-complete
                                                cider.nrepl.middleware.format/wrap-format
                                                cider.nrepl.middleware.info/wrap-info
                                                cider.nrepl.middleware.inspect/wrap-inspect
                                                cider.nrepl.middleware.macroexpand/wrap-macroexpand
                                                cider.nrepl.middleware.ns/wrap-ns
                                                cider.nrepl.middleware.pprint/wrap-pprint
                                                cider.nrepl.middleware.pprint/wrap-pprint-fn
                                                cider.nrepl.middleware.refresh/wrap-refresh
                                                cider.nrepl.middleware.resource/wrap-resource
                                                cider.nrepl.middleware.stacktrace/wrap-stacktrace
                                                cider.nrepl.middleware.test/wrap-test
                                                cider.nrepl.middleware.trace/wrap-trace
                                                cider.nrepl.middleware.out/wrap-out
                                                cider.nrepl.middleware.undef/wrap-undef
                                                cider.nrepl.middleware.version/wrap-version]}
              }
             :uberjar {
                       :aot :all
                       }}
                                        ;:prep-tasks ["compile" ["cljsbuild" "once"]]
  :source-paths ["src/clj" "src/cljs" "src/dev"]
  :clean-targets ^{:protect false} ["resources/public/js" "target"])

