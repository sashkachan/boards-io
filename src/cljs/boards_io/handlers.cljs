(ns boards-io.handlers
  (:require [om.next :as om]
            [goog.dom :as gdom]
            [goog.log :as glog]
            [boards-io.logger :as l]
            [boards-io.update-order :as uo]
            [goog.dom.forms :as forms]
            [clojure.string :as str]))

(declare start-loading)

(defn change-route! [{:keys [reconciler matcher this] :as env} route]
  (let [{:keys [handler route-params]} (matcher route)]
    (om/transact! reconciler `[(local/route! {:route [~handler ~route-params]})])
    (om/transact! reconciler (om/transform-reads reconciler [:route/data]))))

(defn modal-open [{:keys [reconciler ref ident]}]
  (om/transact! reconciler `[(local/toggle-field! {:field ~ref :field-state 1 :ident ~ident})]))

(defn modal-close [{:keys [reconciler ref]} ]
  (om/transact! reconciler `[(local/toggle-field! {:field ~ref :field-state 0})]))

(defn new-board-save [{:keys [reconciler save-btn-field idents] :as env}]
  (let [form (gdom/getElement "new-board-form")
        title (forms/getValueByName form "board-title")]
    (when-not (str/blank? title)
      (om/transact! reconciler
                    (into [`(save/new-board! {:title ~title})]
                          (om/transform-reads reconciler [:board/list])))
      (modal-close (assoc env :ref :board/new-board-modal)))))

(defn new-task-save [{:keys [reconciler save-btn-field extras] :as env}]
  ; todo: if nil? extras -> exception!
  (let [st @reconciler
        column-id (:column-id extras)
        form (gdom/getElement "new-task-form")
        title (forms/getValueByName form "task-title")
        max-order (apply max (map (fn [[_ task]] (:task/order task)) (get-in st [:task/by-id])))
        order (if (nil? max-order) 1 (+ 1 max-order))]
    (om/transact! reconciler
                  (into [
                         `(save/new-task! {:title ~title :column-id ~column-id :order ~order})
                         ]
                        (om/transform-reads reconciler [:column/list])))
    (modal-close env)))

(defn new-column-save [{:keys [reconciler save-btn-field extras] :as env}]
                                        ; todo: if nil? extras -> exception!
  (let [st @(om/get-reconciler reconciler)
        board-id (:board/by-id extras)
        _ (println "my extras " extras)
        form (gdom/getElement "new-column-form")
        title (forms/getValueByName form "column-title")
        max-order (apply max (map (fn [[_ column]] (:column/order column)) (get-in st [:column/by-id])))
        order (if (nil? max-order) 1 (+ 1 max-order))]
    (om/transact! reconciler
                  (into [`(save/new-column! {:title ~title :board-id ~board-id :order ~order})]
                        (om/transform-reads reconciler [:column/list])))
    (modal-close env)))

(defn drag-start [{:keys [reconciler component entity ident ] :as env}]
  (om/transact! reconciler
                `[(local/toggle-field! {:field ~entity :field-state :drag-start :ident ~ident})]))

(defn drag-end-task [{:keys [reconciler component ident] :as env}]
  (let [st @reconciler
        moving-task (get-in st [:app/local-state :field-idents :task/moving])]
    (om/transact! reconciler
                  `[(local/toggle-field! {:field :task/moving :field-state :drag-end :ident ~moving-task})
                    (save/update-order-tasks! {:tasks ~(into [] (vals (:task/by-id @reconciler)))})])))

(defn drag-end-column [{:keys [reconciler component ident columns] :as env}]
  (let [st @reconciler
        columns (get-in st [:column/by-id])
        new-cols (into [] (map
                           (fn [[cid column]]
                             {:db/id cid
                              :column/order (:column/order column)}) columns))]
    (om/transact! reconciler
                  `[(local/toggle-field! {:field :column/moving :field-state :drag-end :ident ~ident})
 
                    (save/update-order-columns! {:columns ~new-cols })]
)))

(defn update-order [{:keys [reconciler component entity entity-id extra]}]
  (let [st @reconciler
        task-dragging? (= :drag-start (get-in st [:app/local-state :task/moving :state]))
        dragged-task-id (get-in st [:app/local-state :field-idents :task/moving :task-id])
        dragged-task-column (uo/taskid->columnid st dragged-task-id)]
    (om/transact! reconciler
                  (cond-> `[(local/update-order! {~entity ~entity-id :extra ~extra})]
                    (and (not= nil dragged-task-column)
                         task-dragging?
                         (= entity :target-column-id)
                         (not= dragged-task-column entity-id))
                    (into `[(save/remove-column-task! {:cid ~dragged-task-column :tid ~dragged-task-id})
                            (save/add-column-task! {:cid ~entity-id :tid ~dragged-task-id})])))))

(defn start-loading [{:keys [reconciler]}]
  (let [st @reconciler]
    (om/transact! reconciler (into `[(local/loading! {:loading-state true})]
                                   (om/transform-reads reconciler [:route/data])))))

(defn stop-loading [{:keys [reconciler]}]
  (om/transact! reconciler `[(local/loading! {:loading-state false})]))

(defn mouse-enter [{:keys [reconciler entity entity-id]}]
  (om/transact! reconciler `[(local/toggle-field! {:field ~entity :field-state :enter :ident {:id ~entity-id}})]))

(defn mouse-leave [{:keys [reconciler entity entity-id]}]
  (om/transact! reconciler `[(local/toggle-field! {:field ~entity :field-state :leave :ident {:id ~entity-id}})]))
