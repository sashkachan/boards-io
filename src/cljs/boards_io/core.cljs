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
    :merge (fn [r s n q]
             (println "merger " s n q)
             {:keys [:route/data]
              :next (let [cur-rd (get s :route/data)
                          new-rd (merge cur-rd n)]
                      (assoc s :route/data new-rd))})
    :send (transit/transit-post "/api")}))

(def env {:reconciler reconciler
          :matcher (partial b/match-route router/router)
          :query-root (c/get-root-query)
          })

(defui Root
  static om/IQuery
  (query [this]
         `[:app/route
           { :app/local-state [*]}
          ~(c/get-root-query)])

  Object
  (componentDidMount
   [this]
   (nav/wire-up (nav/new-history) #(h/change-route! (assoc env :this this) %)))

  (componentDidUpdate [this prev-props prev-state]
                      (println "Root component updated " prev-props))
  
  (render [this]
          (println "root data " (om/props this))
          (let [{:keys [app/route route/data]} (om/props this)
                pr (first route)]
            (if (not= nil pr)
              ((c/route->factory pr) (get data pr)))
            )))

(om/add-root! reconciler Root (js/document.getElementById "app"))
