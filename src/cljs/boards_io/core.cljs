(ns boards-io.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [boards-io.parser :as parser]
            [boards-io.transit :as transit]
            [om.dom :as dom]
            [om.util :as util]
            [cljs.core.async :as async]
            [bidi.bidi :as b] 
            [clojure.string :as string]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [boards-io.router :as router]
            [boards-io.navigation :as nav]
            [boards-io.components :as comps]
            )
  (:import goog.History ))

(enable-console-print!)

(def route->component
  {:boards comps/BoardList
   :columns comps/ColumnList})

(def route->factory
  (zipmap (keys route->component)
          (map om/factory (vals route->component))))

(defn get-root-query []
  {:route/data (zipmap (keys route->component)
                       (map om/get-query (vals route->component)))})

(defui Root
  static om/IQuery
  (query [this ]
         `[:app/route
          ~(get-root-query)])
  
  Object
  (componentDidMount
   [this]
   (println "Root mounted"))
  
  (render [this]
          (println "root data " (:route/data (om/props this)) )
          (let [{:keys [app/route route/data]} (om/props this)]
            (if-not (nil? route)
              ((route->factory (first route)) data)))))

(def reconciler
  (om/reconciler
   {:state (atom {})
    :parser (om/parser {:read parser/read :mutate parser/mutate})
    :send (transit/transit-post "/api")}))

(defn change-route! [route]
  (let [match (:handler (b/match-route router/router route))]
    (println "change-route! pre-transact query: " (get-root-query))
    (om/transact! reconciler `[(change/route! {:route [~match]})
                               ~(get-root-query)
                               ])))

(nav/wire-up (nav/new-history) #(change-route! %))
(om/add-root! reconciler Root (js/document.getElementById "app"))
