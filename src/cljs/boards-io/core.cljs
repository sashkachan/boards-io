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
            )
  (:import goog.History ))

(enable-console-print!)

(defui ColumnList
  static om/IQueryParams
  (params [this]
          {:board-id "0"})
  static om/IQuery
  (query [this]
        '[{:column/list [:column/name {:column/board [:board/name :board/description]}] :board-id ?board-id}])  

  Object
  (render [this] 
          (apply dom/div nil
                 (vec (map
                      (fn [c]
                        (let [{:keys [column/name column/description column/board]} c]
                          (dom/div nil (str name " - " (:board/name board)))))
                      (:column/list (om/props this)))
                     ))))


(defui BoardItem
  static om/Ident
  (ident [_ item]
         [:board/by-name (:board/name item)])
  static om/IQuery
  (query [this]
         [:db/id :board/name :board/description])
  Object
  (render [this]
          (let [{:keys [db/id board/name board/description]} (om/props this)]
            (dom/div nil (dom/a #js { :href (b/path-for router/router :column/list :board-id id)} name)
                     (dom/p nil description)))))

(def board-item (om/factory BoardItem))

(defui BoardList
  static om/IQuery
  (query [this]
         [{:board/list (om/get-query BoardItem)}])
  static Object
  (render [this]
          (println (om/props this))
          (dom/div nil
                   (apply
                    dom/div #js { :className "row"}
                    (-> (map #(board-item %) (:board/list (om/props this)))
                        vec)))))

(def route->component
  {:board/list BoardList
   :column/list ColumnList})

(def route->factory
  (zipmap (keys route->component)
    (map om/factory (vals route->component))))

(defui Root
  static om/IQuery
  (query [this ]
         [:app/route
          {:route/data (zipmap (keys route->component)
                               (map om/get-query (vals route->component)))}])
  
  Object
  (render [this]
          ;;(println (om/props this))
          (let [{:keys [app/route route/data]} (om/props this)]
            ((route->factory (first route)) data))))

(def reconciler
  (om/reconciler
   {:state (atom {:app/route [:board/list]})
    :parser (om/parser {:read parser/read :mutate parser/mutate})
    :send (transit/transit-post "/api")}))

(defn change-route! [route]
  (let [match (:handler (b/match-route router/router route))]
    (println "match:" match)
    (om/transact! reconciler `[(change/route! {:route [~match]})])))

(nav/wire-up (nav/new-history) #(change-route! %))

(om/add-root! reconciler Root  (js/document.getElementById "app"))
