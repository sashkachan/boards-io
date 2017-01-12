(ns boards-io.handlers
  (:require [om.next :as om]
            [goog.dom :as gdom]
            [goog.log :as glog]
            [boards-io.logger :as l]
            [goog.dom.forms :as forms]))

(declare start-loading)

(defn change-route! [{:keys [reconciler matcher this] :as env} route]
  (let [{:keys [handler route-params]} (matcher route)]
    (glog/info l/*logger* "change-route! pre")
    (om/transact! reconciler `[(local/route! {:route [~handler ~route-params]})])
    (glog/info l/*logger* "change-route! post")
    (om/transact! reconciler (om/transform-reads reconciler [:route/data]))
    (glog/info l/*logger* "change-route! reread")))

(defn modal-open [{:keys [reconciler ref ident]}]
  (om/transact! reconciler
   `[(local/toggle-field! {:field ~ref :field-state 1 :ident ~ident})]))

(defn modal-close [{:keys [reconciler ref]} ]
  (om/transact! reconciler `[(local/toggle-field! {:field ~ref :field-state 0})]))

(defn new-board-save [{:keys [reconciler save-btn-field idents] :as env}]
  (let [form (gdom/getElement "new-board-form")
        title (forms/getValueByName form "board-title")
        description (forms/getValueByName form "board-description")]
    (om/transact! reconciler
                  (into [`(local/toggle-field! {:field ~save-btn-field :field-state :off})
                         `(save/new-board! {:title ~title :description ~description})
                         `(local/toggle-field! {:field ~save-btn-field :field-state :on})]
                        (om/transform-reads reconciler [:route/data])))
    (modal-close (assoc env :ref :board/new-board-modal))))

(defn new-task-save [{:keys [reconciler save-btn-field extras] :as env}]
  ; todo: if nil? extras -> exception!
  (let [column-id (:column-id extras)
        form (gdom/getElement "new-task-form")
        title (forms/getValueByName form "task-title")]
    (om/transact! reconciler
                  (into [`(local/toggle-field! {:field ~save-btn-field :field-state :off})
                         `(save/new-task! {:title ~title :column-id ~column-id})
                         `(local/toggle-field! {:field ~save-btn-field :field-state :on})]
                        (om/transform-reads reconciler [:route/data])))
    (modal-close env)))

(defn new-column-save [{:keys [reconciler save-btn-field extras] :as env}]
                                        ; todo: if nil? extras -> exception!
  (let [st @reconciler
        board-id (:board-id extras)
        form (gdom/getElement "new-column-form")
        title (forms/getValueByName form "column-title")
        max-order (apply max (map (fn [[_ column]] (:column/order column)) (get-in st [:route/data :columns :column/by-id])))
        order (if (nil? max-order) 1 (+ 1 max-order))]
    (om/transact! reconciler
                  (into [`(local/toggle-field! {:field ~save-btn-field :field-state :off})
                         `(save/new-column! {:title ~title :board-id ~board-id :order ~order})
                         `(local/toggle-field! {:field ~save-btn-field :field-state :on})]
                        (om/transform-reads reconciler [:route/data])))
    (modal-close env)))

(defn drag-start [{:keys [reconciler component entity ident ] :as env}]
  (om/transact! reconciler
                `[(local/toggle-field! {:field ~entity :field-state :drag-start :ident ~ident})]))

(defn drag-end-task [{:keys [reconciler component ident] :as env}]
  (om/transact! reconciler
                (into `[(local/toggle-field! {:field :task/moving :field-state :drag-end :ident ~ident})]
                      (om/transform-reads reconciler [:route/data]))))

(defn drag-end [{:keys [reconciler component ident columns] :as env}]
  (let [st @reconciler
        columns (get-in st [:route/data :columns :column/by-id])
        new-cols (into [] (map (fn [[cid column]] {:db/id cid
                                                  :column/order (:column/order column)}) columns))]
    (om/transact! reconciler
                  (into `[(save/update-order! {:columns ~new-cols})
                          (local/toggle-field! {:field :column/moving :field-state :drag-end :ident ~ident})]
                        (om/transform-reads reconciler [:route/data])))))

(defn update-order [{:keys [reconciler component entity entity-id]}]
  (om/transact! reconciler `[(local/update-order! {~entity ~entity-id})]))

(defn start-loading [{:keys [reconciler]}]
  (let [st @reconciler]
    (om/transact! reconciler (into `[(local/loading! {:loading-state true})]
                                   (om/transform-reads reconciler [:route/data])))))

(defn stop-loading [{:keys [reconciler]}]
  #_(println "send r " @reconciler)
  (om/transact! reconciler `[(local/loading! {:loading-state false})]))
