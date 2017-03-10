(ns boards-io.system
  (:require [com.stuartsierra.component :as c]
            [ring.adapter.jetty :refer [run-jetty]]
            [boards-io.handler :as handler]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.tools.namespace.repl :as nmr]))

(def ext-config (edn/read-string (slurp (io/resource (get (System/getenv) "CONFIG_EDN_LOCATION" "config.edn")))))

(def system-config
  (merge
   {:port 8888}
   ext-config))

(defrecord WebServer [port connection]
  c/Lifecycle
  (start [component]
    (let [req-handler (handler/top-handler system-config)
          container (run-jetty req-handler
                               {:port port :join? false})]
      (assoc component :container container)))
  (stop [component]
    (if-not (nil? (:container component))
      (.stop (:container component)))
    (dissoc component :container)))


(defn app-system [config-opts]
  (let [{:keys [uri schema-tx init-data port]} config-opts]
    (c/system-map
      :server  (map->WebServer {:port port}))))

(def system (app-system system-config))

(defn system-start []
  (alter-var-root #'system c/start))

(defn system-stop []
  (alter-var-root #'system c/stop))

(defn system-restart []
  (system-stop)
  (nmr/refresh :after 'boards-io.system/system-start))
