(ns boards-io.system
  (:require [com.stuartsierra.component :as c]
            [ring.adapter.jetty :refer [run-jetty]]
            [boards-io.handler :as handler]
            [clojure.tools.namespace.repl :as nmr]))

(def system-config
  {
   :port 8888})

(defrecord WebServer [port connection]
  c/Lifecycle
  (start [component]
    (let [req-handler (handler/top-handler)
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
