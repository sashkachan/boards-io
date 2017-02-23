(ns boards-io.core
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [boards-io.parser :as parser]
            [boards-io.transit :as transit]
            [om.dom :as dom]
            [om.util :as util]
            [goog.log :as glog]
            [boards-io.logger :as l]
            [cljs.core.async :as async]
            [bidi.bidi :as b] 
            [clojure.string :as string]
            [goog.events :as events]
            [goog.history.EventType :as EventType]
            [boards-io.router :as router]
            [boards-io.navigation :as nav]
            [boards-io.components :as c]
            [boards-io.handlers :as h]
            [om.next.protocols :as p])
  (:import goog.History ))


(enable-console-print!)

(defonce state {:app/route []})

(def reconciler
  (om/reconciler
   {:state state
    :parser (om/parser {:read parser/read :mutate parser/mutate})
    :normalize true
    :id-key :db/id
    ;:logger 
    ;:merge  (parser/merger c/route->component)
    :send (transit/transit-post "/api")}))

(def env {:reconciler reconciler
          :matcher (partial b/match-route router/router)
          :query-root (c/get-root-query)
          })

(defui Root
  static om/IQuery
  (query [this]
         (into `[:app/route
                 ~(c/get-root-query)]
               (om/get-query c/AuthHeader)))

  Object
  (componentDidMount
   [this]
   (nav/wire-up (nav/new-history) #(h/change-route! (assoc env :this this) %)))

  (render [this]
          (let [{:keys [app/route route/data app/local-state oauth/user]} (om/props this)
                pr (first route)
                comp-data (if (not= nil pr)
                            (let [component ((c/route->factory pr) (get data pr))]
                              component))]
            (dom/div nil [((om/factory c/Header {:keyfn identity}) {:oauth/user (:oauth/user (om/props this))} )
                          (dom/div #js {:key "route-container" :className "container-fluid"}
                                   comp-data)
                          ]))))

(om/add-root! reconciler Root (js/document.getElementById "app"))
