(ns boards-io.handler
  (:require [ring.util.response :refer [response file-response resource-response redirect]]
            [clojure.edn :as edn]
            [boards-io.middleware
             :refer [wrap-transit-body wrap-transit-response
                     wrap-transit-params]]   
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [clj-http.client :as http]
            [bidi.bidi :as bidi]
            [cheshire.core :refer [parse-string]]
            [om.next.server :as om]
            [clj-oauth2.client :as oauth2]
            [boards-io.html :as html]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]   
            [clojure.walk :as walk]
            [boards-io.parser :as parser]
            [clj-oauth2.client :as oauth2]
            [clojure.java.io :as io]))

(declare top-handler)

(def ext-config (edn/read-string (slurp (io/resource "oauth-config.edn"))))

(def login-uri
  (get (System/getenv) "LOGIN_URI" "https://accounts.google.com"))

(def google-com-oauth2
  (merge {:authorization-uri (str login-uri  "/o/oauth2/auth")
          :access-token-uri (str login-uri "/o/oauth2/token")
          :redirect-uri (get (System/getenv) "REDIRECT_URI" "http://localhost:9091/auth") 
          :client-id (get (System/getenv) "CLIENT_ID" "")
          :client-secret (get (System/getenv ) "CLIENT_SECRET" "")
          :access-query-param :access_token
          :scope ["https://www.googleapis.com/auth/userinfo.email"]
          :grant-type "authorization_code"
          :access-type "online"
          :approval_prompt ""}
         ext-config))

(defn auth-req []
  (oauth2/make-auth-request google-com-oauth2))

(defn- google-access-token [request]
  (oauth2/get-access-token google-com-oauth2 (:params request) auth-req))

(defn- google-user-email [access-token]
  (let [response (oauth2/get "https://www.googleapis.com/oauth2/v1/userinfo" {:oauth access-token})]
    (get (parse-string (:body response)) "email")))


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
    (generate-response data')))


(def routes
  ["" {"/oauth" :oauth
       "/api" :api
       "/auth" :oauth-callback
       #".*" :index}])


(defn wrap-connection [handler conn]
  (fn [req]
    (handler (assoc req :datomic-connection conn))))

;; main handler

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req)
                                :request-method (:request-method req))]
    (case (:handler match)
      :index (index req)
      :api   (api req (:route-params match))
      :oauth-callback (let [token (oauth2/get-access-token
                                   google-com-oauth2
                                   (:params req)
                                   (auth-req))
                            token-info (:body (http/get "https://www.googleapis.com/oauth2/v1/tokeninfo"
                                                        {:query-params {:access_token (:access-token token)}
                                                         :as :json}))
                            _ (parser/save-token token-info (:access-token token) (:datomic-connection req))
                            resp (merge (index req)
                                        {:cookies {"authToken" {:value (:access-token token)
                                                                 :max-age (:expires_in (:params token))}}})]
                        resp)
      :oauth (redirect (:uri (auth-req)))
      req)))

(defn top-handler [conn]
  (-> handler
      wrap-cookies
      wrap-session
      wrap-keyword-params
      wrap-params
      (wrap-connection conn)
      wrap-transit-params
      wrap-transit-response
      (wrap-resource "public")))
