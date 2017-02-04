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
    (glog/info l/*logger*  route-params)
    (om/transact! reconciler `[(local/route! {:route [~handler ~route-params]})])
    (glog/info l/*logger* "change-route! post" )
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
  (let [st @reconciler
        column-id (:column-id extras)
        form (gdom/getElement "new-task-form")
        title (forms/getValueByName form "task-title")
        max-order (apply max (map (fn [[_ task]] (:task/order task)) (get-in st [:route/data :columns :task/by-id])))
        order (if (nil? max-order) 1 (+ 1 max-order))]
    (om/transact! reconciler
                  (into [`(local/toggle-field! {:field ~save-btn-field :field-state :off})
                         `(save/new-task! {:title ~title :column-id ~column-id :order ~order})
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
  (let [st @reconciler
        cols (get-in st [:route/data :columns :column/by-id])
        task-pref [:route/data :columns]
        moving-task (get-in st [:route/data :columns :app/local-state :field-idents :task/moving])
        new-tasks (reduce
                   (fn [inv [cid col]]
                     (into inv
                           (mapv
                              (fn [ident]
                                (assoc-in (get-in st (into task-pref ident)) [:task/column :db/id] cid))
                              (:task/_column col)))) [] cols)]
    (om/transact! reconciler
                  `[(local/toggle-field! {:field :task/moving :field-state :drag-end :ident ~moving-task})
                    (save/update-order-tasks! {:tasks ~new-tasks })])))

(defn drag-end-column [{:keys [reconciler component ident columns] :as env}]
  (let [st @reconciler
        columns (get-in st [:route/data :columns :column/by-id])
        new-cols (into [] (map (fn [[cid column]] {:db/id cid
                                                  :column/order (:column/order column)}) columns))]
    (om/transact! reconciler
                  `[(local/toggle-field! {:field :column/moving :field-state :drag-end :ident ~ident})
 
                    (save/update-order-columns! {:columns ~new-cols })]
)))

(defn update-order [{:keys [reconciler component entity entity-id extra]}]
  (om/transact! reconciler
                (into `[(local/update-order! {~entity ~entity-id :extra ~extra})]
                      #_(om/transform-reads reconciler [:route/data]))))

(defn start-loading [{:keys [reconciler]}]
  (let [st @reconciler]
    (om/transact! reconciler (into `[(local/loading! {:loading-state true})]
                                   (om/transform-reads reconciler [:route/data])))))

(defn stop-loading [{:keys [reconciler]}]
  #_(println "send r " @reconciler)
  (om/transact! reconciler `[(local/loading! {:loading-state false})]))

(defn mouse-enter [{:keys [reconciler entity entity-id]}]
  (om/transact! reconciler `[(local/toggle-field! {:field ~entity :field-state :enter :ident {:id ~entity-id}})]))

(defn mouse-leave [{:keys [reconciler entity entity-id]}]
  (om/transact! reconciler `[(local/toggle-field! {:field ~entity :field-state :leave :ident {:id ~entity-id}})]))
