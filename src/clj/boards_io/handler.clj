(ns boards-io.handler
  (:require [ring.util.response :refer [response file-response resource-response redirect]]
            [ring.middleware.resource :refer [wrap-resource]]
            [puppetlabs.ring-middleware.core :refer [wrap-proxy]]
            [bidi.bidi :as bidi]
            [boards-io.html :as html]
))

(declare top-handler)



(defn index [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html/index)})

;; main handler

(defn handler [req]
  (index req))

(def local-routes
  ["api" "oauth" "auth"])

(defn top-handler [ext-config]
  (-> handler
      (wrap-resource "public")
      (wrap-proxy
       (re-pattern (str "^/" (reduce #(str %1 "|" %2)  local-routes)))
       (get ext-config :api))))
