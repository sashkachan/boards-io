(ns boards-io.handler
  (:require [ring.util.response :refer [response file-response resource-response redirect]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.format :refer [wrap-restful-format]]
            [ring.middleware.format-params :refer [wrap-transit-json-params]]
            [ring.middleware.format-response :refer [wrap-transit-json-response]]
            [clj-http.client :as http]
            [bidi.bidi :as bidi]
            [cheshire.core :refer [parse-string]]
            [om.next.server :as om]
            [clj-oauth2.client :as oauth2]
            [clojure.tools.namespace.repl :refer (refresh refresh-all)]
            [boards-io.parser :as parser]
            [boards-io.html :as html]
            [clj-oauth2.client :as oauth2]))

(declare top-handler)

(def login-uri
  (get (System/getenv) "LOGIN_URI" "https://accounts.google.com"))

(defn google-com-oauth2 [ext-config]
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

(defn- google-access-token [request auth-req]
  (oauth2/get-access-token google-com-oauth2 (:params request) auth-req))

(defn- google-user-email [access-token]
  (let [response (oauth2/get "https://www.googleapis.com/oauth2/v1/userinfo" {:oauth access-token})]
    (get (parse-string (:body response)) "email")))

(defn- google-com-oauth2 [ext-config]
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

(defn auth-req [ext-config]
  (oauth2/make-auth-request (google-com-oauth2 ext-config)))
;;;;;;;;;;;;;;;; HANDLERS

(defn index [req]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (html/index)})

(defn generate-response [data & [status]]
  {:status  (or status 200)
   :headers {"Content-Type" "application/transit+json"}
   :body    data})

(defn api [req route-params]
  (let [parser (om/parser {:read parser/readf :mutate parser/mutatef})
        data (try (parser
                    {:auth-token (get-in req [:cookies "authToken" :value])
                     :conn (:datomic-connection req)
                     :route-params route-params} (:body-params req))
                  (catch Exception e
                    (do (println e)
                        (.getMessage e))))]
    (generate-response data)))


(def routes
  ["" {"/oauth" :oauth
       "/api" :api
       "/auth" :oauth-callback
       #".*" :index}])


(defn wrap-connection [handler conn]
  (fn [req]
    (handler (assoc req :datomic-connection conn))))

(defn wrap-ext-config [handler ext-config]
  (fn [req]
    (handler (assoc req
               :goog-com-auth (google-com-oauth2 ext-config)
               :ext-config ext-config))))
;; main handler

(defn wrap-exception [f]
  (fn [request]
    (try (f request)
         (catch Exception e
           (do
             (println "Exception: " e)
             {:status 500
              :body {}})))))

(defn handler [req]
  (let [match (bidi/match-route routes (:uri req)
                                :request-method (:request-method req))]
    (case (:handler match)
      :index (index req)
      :api   (api req (:route-params match))
      :oauth-callback (let [token (oauth2/get-access-token
                                    (:goog-com-auth req)
                                    (:params req)
                                    (auth-req (:ext-config req)))
                            token-info (:body (http/get "https://www.googleapis.com/oauth2/v1/tokeninfo"
                                                        {:query-params {:access_token (:access-token token)}
                                                         :as :json}))
                            _ (parser/save-token token-info (:access-token token) (:datomic-connection req))
                            resp (merge {:status 302
                                         :headers {"Location" "/"}}
                                        {:cookies {"authToken" {:value (:access-token token)
                                                                :max-age (:expires_in (:params token))}}})]
                        resp)
      :oauth (redirect (:uri (auth-req (:ext-config req))))
      req)))

(defn top-handler [conn ext-config]
  (-> handler
      wrap-cookies
      wrap-session
      wrap-keyword-params
      wrap-params
      (wrap-connection conn)
      (wrap-ext-config ext-config)
      (wrap-restful-format :formats [:transit-json])
      (wrap-resource "public")
      wrap-exception))
