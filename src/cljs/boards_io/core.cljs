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
            [boards-io.components :as c]
            [boards-io.handlers :as h]
            )
  (:import goog.History ))

(enable-console-print!)

(def reconciler
  (om/reconciler
   {:state (atom {:app/route [] :app/local-state {}})
    :parser (om/parser {:read parser/read :mutate parser/mutate})
    #_:merge #_(fn [r s n q]
             (println "merger " s n q)
             {:keys [:route/data]
              :next (assoc s :route/data n)})
    :send (transit/transit-post "/api")}))

(def env {:reconciler reconciler
          :matcher (partial b/match-route router/router)
          :query-root (c/get-root-query)})

(defn change-route! [route]
  (let [{:keys [handler route-params]} (b/match-route router/router route)]
    (println "change-route! pre-transact: " handler route-params)
    (om/transact! reconciler `[(change/route! {:route [~handler ~route-params]})
                               ~(om/force (get (c/get-root-query) handler))
                               ])))

(nav/wire-up (nav/new-history) #(h/change-route! env %))
(om/add-root! reconciler c/Root (js/document.getElementById "app"))

