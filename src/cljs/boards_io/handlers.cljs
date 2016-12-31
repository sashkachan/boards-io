(ns boards-io.handlers
  (:require [om.next :as om]
            [goog.dom :as gdom]
            [goog.log :as glog]
            [boards-io.logger :as l]
            [goog.dom.forms :as forms]))

(defn change-route! [{:keys [reconciler matcher query-root this]} route]
  (let [{:keys [handler route-params]} (matcher route)]
    (glog/info l/*logger* "change-route! pre")
    (om/transact! reconciler `[
                               (local/route! {:route [~handler ~route-params]})
                               ~query-root
                               
                               ])
    (glog/info l/*logger* "change-route! post"))
  )

(defn modal-open [{:keys [reconciler ref ident]}]
  (om/transact! reconciler
   `[(local/toggle-field! {:field ~ref :field-state 1 :ident ~ident})]))

(defn modal-close [{:keys [reconciler ref]} ]
  (om/transact! reconciler `[(local/toggle-field! {:field ~ref :field-state 0})]))

(defn new-board-save [{:keys [reconciler root-query save-btn-field idents] :as env}]
  (println "Root-query " root-query save-btn-field)
  (let [form (gdom/getElement "new-board-form")
        title (forms/getValueByName form "board-title")
        description (forms/getValueByName form "board-description")]
    (om/transact! reconciler
                  `[(local/toggle-field! {:field ~save-btn-field :field-state :off})
                    (save/new-board! {:title ~title :description ~description})
                    ~root-query
                    (local/toggle-field! {:field ~save-btn-field :field-state :on})
                    ])
    (modal-close (assoc env :ref :board/new-board-modal))))

(defn new-task-save [{:keys [reconciler root-query save-btn-field extras] :as env}]
  ; todo: if nil? extras -> exception!
  (let [column-id (:column-id extras)
        form (gdom/getElement "new-task-form")
        title (forms/getValueByName form "task-title")]
    (om/transact! reconciler
                  `[(local/toggle-field! {:field ~save-btn-field :field-state :off})
                    (save/new-task! {:title ~title :column-id ~column-id})
                    ~root-query
                    (local/toggle-field! {:field ~save-btn-field :field-state :on})
                    ])
    (modal-close env)))

(defn new-column-save [{:keys [reconciler root-query save-btn-field extras] :as env}]
                                        ; todo: if nil? extras -> exception!
  (let [st @reconciler
        board-id (:board-id extras)
        form (gdom/getElement "new-column-form")
        title (forms/getValueByName form "column-title")
        max-order (apply max (map #(:column/order %) (-> st :route/data :columns :column/list)))
        order (if (nil? max-order) 1 (+ 1 max-order))]
    (om/transact! reconciler
                  `[(local/toggle-field! {:field ~save-btn-field :field-state :off})
                    (save/new-column! {:title ~title :board-id ~board-id :order ~order})
                    ~root-query
                    (local/toggle-field! {:field ~save-btn-field :field-state :on})])
    (modal-close env)))

(defn drag-start [{:keys [reconciler root-query ident] :as env}]
  (om/transact! reconciler
                `[(local/toggle-field! {:field :column/moving :field-state :drag-start :ident ~ident})]))

(defn drag-end [{:keys [reconciler component root-query ident columns] :as env}]
  (let [st @reconciler
        columns (-> st :route/data :columns :column/list vec)
        new-cols (map (fn [column] (-> column
                                      (dissoc :column/board)
                                      (dissoc :task/_column))) columns)]
    (om/transact! reconciler
                  `[(save/update-order! {:columns ~new-cols})
                    (local/toggle-field! {:field :column/moving :field-state :drag-end :ident ~ident})])))

(defn start-loading [{:keys [reconciler root-query]}]
  (let [st @reconciler]
    (om/transact! reconciler `[(local/loading! {:loading-state true}) ~root-query])))

(defn stop-loading [{:keys [reconciler root-query]}]
  (om/transact! reconciler `[(local/loading! {:loading-state false})] ~root-query))
