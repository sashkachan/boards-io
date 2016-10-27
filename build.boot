(set-env!
 :source-paths    #{"src/clj" "src/cljs"}
 :resource-paths  #{"resources"}
 :dependencies '[[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.89"]
                 [org.clojure/core.async "0.2.385"]
                 [cljsjs/react "15.2.1-1"]
                 [cljsjs/react-dom "15.2.1-1"]
         ;        [org.clojure/tools.namespace "0.2.11"]
                 [org.clojure/tools.logging   "0.3.1"]
                 [com.datomic/datomic-free    "0.9.5394"]
                 [com.stuartsierra/component  "0.3.1"]
                 [bidi "2.0.11"]
                 [com.cognitect/transit-cljs  "0.8.239"]
                 [com.cognitect/transit-clj   "0.8.288"]
                 [ring "1.5.0"]
                 [org.omcljs/om "1.0.0-alpha46"]
                 ;;[figwheel-sidecar "0.5.4-6"]
                 ;;[com.cemerick/piggieback "0.2.1"]
                 [hiccup "1.0.5"]
                 ;;
                 [org.danielsz/system         "0.3.2-SNAPSHOT"
                  :exclusions [org.clojure/clojure]]
                 [boot-environ "1.1.0"]
                 [com.cemerick/piggieback     "0.2.1"          :scope "test"
                  :exclusions [org.clojure/clojure org.clojure/clojurescript]]
                 [adzerk/boot-cljs            "1.7.228-1"      :scope "test"]
                 [adzerk/boot-cljs-repl       "0.3.3"          :scope "test"]
                 [adzerk/boot-reload          "0.4.12"         :scope "test"]
                 [org.clojure/tools.nrepl     "0.2.12"         :scope "test"
                  :exclusions [org.clojure/clojure]]
                 [weasel                      "0.7.0"          :scope "test"
                  :exclusions [org.clojure/clojure org.clojure/clojurescript]]
                 [org.clojure/tools.nrepl "0.2.12"]
                 
                 [environ                     "1.1.0"
                  :exclusions [org.clojure/clojure]]
                 [pandeiro/boot-http          "0.7.1-SNAPSHOT" :scope "test"]
                 ]
 )

(load-data-readers!)

(require
 '[adzerk.boot-cljs      :refer [cljs]]
 '[boot.repl]
 '[adzerk.boot-cljs-repl :as cr :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[pandeiro.boot-http    :refer [serve]]
 '[system.boot :refer [system run]]
 '[environ.boot :refer [environ]]
 '[boards-io.system :refer [dev-system]])

(deftask deps [])

(deftask cider ""
  []
  (reset! boot.repl/*default-dependencies*
         '[[org.clojure/tools.nrepl "0.2.12"]
           [cider/cider-nrepl "0.14.0-SNAPSHOT"]
           [refactor-nrepl "2.3.0-SNAPSHOT"]])
  (reset! boot.repl/*default-middleware*
          ['cider.nrepl/cider-middleware
           'refactor-nrepl.middleware/wrap-refactor]))

(deftask dev []
  (comp
    (watch)
    (system :sys #'dev-system :auto true :files ["handler.clj" "parser.clj" "html.clj"])
    (cljs-repl)
    (reload)
    (speak)
    (cljs :source-map true
          :compiler-options {;:asset-path "/js/"
                             :source-map-timestamp true
                             :parallel-build true
                             :compiler-stats true}
          :ids #{"public/js/app"})
    
    (target)))
