(ns boards-io.handler
  (:require [ring.util.response :refer [response file-response resource-response redirect]]
            [clojure.edn :as edn]   
            [ring.middleware.resource :refer [wrap-resource]]
            [puppetlabs.ring-middleware.core :refer [wrap-proxy]]
            [bidi.bidi :as bidi]
            [boards-io.html :as html]
            [clojure.java.io :as io]))

(declare top-handler)

(def ext-config (edn/read-string (slurp (io/resource (get (System/getenv) "CONFIG_EDN_LOCATION" "config.edn")))))


(defn index [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html/index)})

;; main handler

(defn handler [req]
  (index req))

(defn top-handler []
  (-> handler
      (wrap-resource "public")
      (wrap-proxy #"^/.+" (get ext-config :api))))
