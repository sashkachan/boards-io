(ns boards-io.handlers
  (:require [om.next :as om]))

(defn change-route! [{:keys [reconciler matcher query-root]} route]
  (let [{:keys [handler route-params]} (matcher route)]
    (println "change-route! pre-transact: " handler route-params)
    (om/transact! reconciler `[(change/route! {:route [~handler ~route-params]})
                               ~(om/force query-root)
                               ])))


(defn new-board [{:keys [reconciler]} ]
  (om/transact! reconciler '[(change/toggle-modal! {:modal :board/new-board-modal :modal-state 1})]))

(defn new-board-close [{:keys [reconciler]}]
  (om/transact! reconciler '[(change/toggle-modal! {:modal :board/new-board-modal :modal-state 0})]))
