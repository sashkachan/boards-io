(ns dev
  (:require [clojure.tools.namespace.repl :as nmr]
            [boards-io.system :as s]
            [com.stuartsierra.component :as c]
            [figwheel-sidecar.repl-api :as ra]
            [figwheel-sidecar.system :as figsys]))

(nmr/disable-reload!)

(def system
  (c/system-map
   :figwheel-system (figsys/figwheel-system (figsys/fetch-config))))

(defn system-start []
  (alter-var-root #'dev/system c/start)
  (boards-io.system/system-start))

(defn system-stop []
  (alter-var-root #'dev/system c/stop)
  (s/system-stop))

(defn system-restart []
  (system-stop)
  (nmr/refresh :after 'dev/system-start))

(defn system-restart-prod []
  (s/system-stop)
  (nmr/refresh :after 's/system-start))

(defn cljs-repl []
  (figsys/cljs-repl (:figwheel-system system)))

(defn -main []
  (system-start))
