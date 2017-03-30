(ns boards-io.html
  (:require [hiccup.core :as hcp]))

(def head
  [:head
   [:meta {:charset "UTF-8"}]
   [:meta
    {:content "width=device-width, initial-scale=1", :name "viewport"}]
   [:link
    {:href
     "/css/bootstrap.min.css",
     :rel "stylesheet"}]
   [:link {:href "https://fonts.googleapis.com/css?family=Lato" :rel "stylesheet"}]
   [:link
    {:type "text/css", :rel "stylesheet", :href "/css/style.min.css"}]])

(def app
  [:div#app])

(defn index []
  (hcp/html
   [:html
    head
    [:body
     app
     [:script {:type "text/javascript", :src "/js/main.js"}]]]))

