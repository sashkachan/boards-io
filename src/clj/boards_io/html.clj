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
     [:link
      {:type "text/css", :rel "stylesheet", :href "/css/style.css"}]])

(def nav
  [:nav {:class "navbar navbar-default" :id "header"}
   [:div {:class "container-fluid"}
    [:div {:class "navbar-header"}
     [:a {:class "navbar-brand" :href "/"} "Boards.io"]]
    [:div {:class "collapse navbar-collapse"}
     [:ul {:class "nav navbar-nav"}
      [:li#boards-list nil
       [:a {:href "#"} "Boards"]]]]]])

(def app
  [:div#app.container-fluid])

(defn index []
  (hcp/html
   [:html
    head
    [:body
     nav
     app
     [:script {:type "text/javascript", :src "/js/main.js"}]]]))

