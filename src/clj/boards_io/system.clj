(ns boards-io.system
  (:require [com.stuartsierra.component :as c]
            [ring.adapter.jetty :refer [run-jetty]]
            [datomic.api :as d]
            [clojure.edn :as edn]
            [boards-io.datomic :as dat-data]
            [boards-io.handler :as handler]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.tools.namespace.repl :as nmr]))

(def ext-config (edn/read-string (slurp (io/resource (get (System/getenv) "CONFIG_EDN_LOCATION" "config.edn")))))

(def system-config
  (merge
   {:schema-tx dat-data/schema-tx
    :init-data dat-data/initial-data
    :port 9082}
   ext-config))

(defrecord Database [uri schema-tx init-data]
  c/Lifecycle
  (start [component]
    (let [_ (d/create-database uri)
          conn (d/connect uri)]
      @(d/transact conn schema-tx)
      #_@(d/transact conn init-data)
      (assoc component :connection conn)))

  (stop [component]
    (when (:connection component)
      #_(d/delete-database uri)
      (d/release (:connection component)))
    (assoc component :connection nil)))

(defrecord WebServer [port connection]
  c/Lifecycle
  (start [component]
    (let [conn (:connection connection)
          _ (println "webserver conn " conn)]
      (let [req-handler (handler/top-handler conn ext-config)
            container (run-jetty req-handler
                                 {:port port :join? false})]
        (assoc component :container container))))
  (stop [component]
    (if-not (nil? (:container component))
      (.stop (:container component)))
    (dissoc component :container)))


(defn app-system [config-opts]
  (let [{:keys [uri schema-tx init-data port]} config-opts]
    (c/system-map
     :db (map->Database {:uri uri :schema-tx schema-tx :init-data init-data})
     :server (c/using (map->WebServer {:port port}) {:connection :db}))))

(def system (app-system system-config))

(defn system-start []
  (alter-var-root #'system c/start))

(defn system-stop []
  (alter-var-root #'system c/stop))

(defn system-restart []
  (system-stop)
  (nmr/refresh :after 'boards-io.system/system-start))
