(ns boards-io.handlers
  (:require [om.next :as om]))

(defn change-route! [{:keys [reconciler matcher query-root this]} route]
  (let [{:keys [handler route-params]} (matcher route)
        ;;root (:root @(:state reconciler))
        ]
    (om/transact! this `[(change/route! {:route [~handler ~route-params]})
                         ~query-root
                         ])))


(defn new-board [{:keys [reconciler]} ]
  (om/transact! reconciler '[(change/toggle-modal! {:modal :board/new-board-modal :modal-state 1})]))

(defn new-board-close [{:keys [reconciler]}]
  (om/transact! reconciler '[(change/toggle-modal! {:modal :board/new-board-modal :modal-state 0})]))
