(ns boards-io.handler
  (:require [ring.util.response :refer [response file-response resource-response]]   
            [boards-io.middleware
             :refer [wrap-transit-body wrap-transit-response
                     wrap-transit-params]]   
            [ring.middleware.resource :refer [wrap-resource]]               
            [bidi.bidi :as bidi]   
            [om.next.server :as om]
            [boards-io.html :as html]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]   
            [clojure.walk :as walk]
            [boards-io.parser :as parser]))

(declare top-handler)

;;;;;;;;;;;;;;;; HANDLERS

(defn index [req]
  (println "serving index")
  (if-let [resp (resource-response (str (:uri req)) {:root "public"})]
    resp
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (html/index)}))

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

(defn api [req route-params]
  (let [parser (om/parser {:read parser/readf :mutate parser/mutatef})
        data (parser
              {:conn (:datomic-connection req) :route-params route-params} (:transit-params req))
        data' (walk/postwalk (fn [x]
                               (if (and (sequential? x) (= :result (first x)))
                                 [(first x) (dissoc (second x) :db-before :db-after :tx-data)]
                                 x))
                             data)]
    (println data')
    (generate-response data')))


(def routes
  ["" {"/api" :api
       #".*" :index}])


(defn wrap-connection [handler conn]
  (fn [req]
    (handler (assoc req :datomic-connection conn))))

;; main handler

(defn handler [req]
  (println req)
  (let [match (bidi/match-route routes (:uri req)
                                :request-method (:request-method req))]
                                        ;(println match)
    (case (:handler match)
      :index (index req)
      :api   (api req (:route-params match))
      req)))

(defn top-handler [conn]
  (wrap-resource
   (wrap-transit-response
    (wrap-transit-params (wrap-connection handler conn)))
   "public"))
