(ns boards-io.handler
  (:require [ring.util.response :refer [response file-response resource-response redirect]]
            [clojure.edn :as edn]   
            [ring.middleware.resource :refer [wrap-resource]]
            [bidi.bidi :as bidi]
            [boards-io.html :as html]
            [clojure.java.io :as io]))

(declare top-handler)

(def ext-config (edn/read-string (slurp (io/resource (get (System/getenv) "CONFIG_EDN_LOCATION" "config.edn")))))


;;;;;;;;;;;;;;;; HANDLERS

(defn index [req]
  (if-let [resp (resource-response (str (:uri req)) {:root "public"})]
    resp
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (html/index)}))

(def routes
  ["" {#".*" :index}])


;; main handler

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req)
                                :request-method (:request-method req))]
    (case (:handler match)
      :index (index req)
      req)))

(defn top-handler []
  (-> handler
      (wrap-resource "public")))
