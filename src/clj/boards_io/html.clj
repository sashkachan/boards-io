(ns boards-io.html
  (:require [hiccup.core :as hcp]))

(defn index []
  (hcp/html
   [:html
    [:head
     [:meta {:charset "UTF-8"}]
     [:meta
      {:content "width=device-width, initial-scale=1", :name "viewport"}]
     [:link
      {:crossorigin "anonymous",
       :integrity
       "sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u",
       :href
       "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css",
       :rel "stylesheet"}]
     [:link
      {:type "text/css", :rel "stylesheet", :href "/css/style.css"}]]
    [:body
     [:div#header ""]
     [:div#app.container]
     [:script {:type "text/javascript", :src "/js/main.js"}]]]))

