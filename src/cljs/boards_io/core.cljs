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
            [om.next.protocols :as p]
            [pushy.core :as pushy]
            [bidi.bidi :as bidi]
            [compassus.core :as compassus])
  (:import goog.History ))


(enable-console-print!)

(defonce state (atom {:app/route []}))

(declare app)

(defn update-route!
  [{:keys [handler] :as route}]
  (let [current-route (compassus/current-route app)]
    (when (not= handler current-route)
      (compassus/set-route! app handler))))

(def history
  (pushy/pushy update-route!
    (partial bidi/match-route router/router)))

(println "routes: " (zipmap (keys c/route->component)
                            (vals c/route->component)))

(def app
  (compassus/application
    ;; :index is the initial route of the application
   {:routes c/route->component
    :index-route :boards
    
    :mixins [(compassus/did-mount #(pushy/start! history))
             (compassus/will-unmount #(pushy/stop! history))]
    :normalize true
    :reconciler (om/reconciler
                 {:state state
                  :parser (compassus/parser {:read parser/read :mutate parser/mutate})
                  :normalize true
                  :id-key :db/id
                                        ;:merge  (parser/merger c/route->component)
                  :send (transit/transit-post "/api")})}
    ))

#_(def env {:reconciler reconciler
          :matcher (partial b/match-route router/router)
          :query-root (c/get-root-query)
          })
(compassus/mount! app (js/document.getElementById "app"))

;(om/add-root! reconciler Root (js/document.getElementById "app"))

