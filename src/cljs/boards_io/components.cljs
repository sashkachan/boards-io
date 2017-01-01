(ns boards-io.components
  (:require [bidi.bidi :as b]
            [boards-io.handlers :as h]
            [boards-io.router :as router]
            [boards-io.modals :as m]
            [om.dom :as dom]
            [goog.events :as events]
            [om.next :as om :refer-macros [defui ui]]))

(declare get-root-query)

(def modal (om/factory m/Modal {:keyfn :ref}))

(defui ColumnTasks
  Object
  (render [this]
          (apply dom/div #js {:className "board-column-tasks"}
                 (vec (map #(dom/div #js {:className "board-column-task-item"} (:task/name %)) (om/props this))))))

(def column-tasks (om/factory ColumnTasks {:keyfn #(str "tasks-" (:db/id (first %)))}))

(defui ColumnItem
  static om/Ident
  (ident [_ item]
         [:column/by-id (-> item :db/id)])
  Object
  (render [this]
          (let [is-moving (-> this om/props :moving)
                class-name (str "board-column " (if is-moving "moving" ""))
                style #js {:order (-> this om/props :column/order) }
                column-id (:db/id (om/props this))
                drag-data-map {:component this
                               :reconciler (om/get-reconciler this)
                               :root-query (get-root-query)
                               :ident {:column (om/props this)}}
                js-map (cond-> {:className class-name
                                :key (str "item-" column-id)
                                :style style
                                :draggable "true"
                                :onDragStart (fn [e] (h/drag-start drag-data-map))
                                :onDragEnd (fn [e] (h/drag-end drag-data-map))}
                        (not is-moving)
                        (assoc :onDragEnter
                               (fn [e] (om/transact! this `[(local/update-order! {:target-column ~(om/props this)})]))))]
            (dom/div (clj->js js-map) 
                     [(dom/div #js {:className "board-column-title" :key (str "item-title-" column-id)} (str (:column/name (om/props this)) ))
                      (column-tasks (:task/_column (om/props this)))
                      (dom/div #js {:className "board-column-new-item" :key (str "new-item-div-" column-id)}
                               (dom/a #js {:href "#" :onClick #(h/modal-open {:reconciler (om/get-reconciler this) :ref :column/new-task-modal :ident {:column-id (:db/id (om/props this))}} )} "New item..." ))]))))

(def column-item (om/factory ColumnItem {:keyfn :db/id}))

(defn column-wrap [props]
  (let [{:keys [moving field-idents]} props
        column-items (:list props)]
    (vec
     (map
      (fn [item]
        (let [mov-col (-> field-idents :column)
              mov-col-id (:db/id mov-col)
              item (cond-> item
                     (and (= moving :drag-start) (= (:db/id item) mov-col-id))
                     (assoc :moving true))]
          (column-item item))) (:list props)))))

(defui ColumnList
  static om/IQueryParams
  (params [this]
          {:board-id 0})
  static om/IQuery
  (query [this]
         '[({:column/list [:db/id :column/name :column/order {:column/board [*]}
                           {:task/_column [*]} {:app/local-state [:column/moving]} ]} {:board-id ?board-id})
           {:app/local-state [:column/moving
                              :column/new-column-modal
                              :column/new-task-modal
                              :column/save-btn-field
                              :field-idents]}
           :app/route])

  Object
  (render [this]
          (let [{:keys [app/local-state]} (om/props this)
                board-id (js/parseInt (-> (om/props this) :app/route second :board-id))
                cols (merge
                      (into []
                            (map
                             (fn [item]
                               (let [mov-col (-> local-state :field-idents :column/moving :column)
                                     mov-col-id (:db/id mov-col)
                                     moving (-> local-state :column/moving :state )
                                     item (cond-> item
                                            (and (= moving :drag-start) (= (:db/id item) mov-col-id))
                                            (assoc :moving true))]
                                 (column-item item))) (:column/list (om/props this))))
                      (dom/div #js {:className "board-column new-column" :key "board-column-new-column"}
                               (dom/a #js {:href "#"
                                           :key "board-column-new-column-href"
                                           :onClick #(h/modal-open
                                                      {:reconciler (om/get-reconciler this)
                                                       :ref :column/new-column-modal
                                                       :ident {:board-id board-id}} )} "New column...")))]
            (dom/div #js {:className "board-wrap" :key (str "board-wrap-" board-id)}
                     (cond-> cols
                       (= 1 (-> local-state :column/new-column-modal :state))
                       (conj (modal {:root-query (get-root-query)
                                     :save-btn-state (-> local-state :column/save-btn-field :state)
                                     :ref :column/new-column-modal
                                     :submit-fn (partial h/new-column-save)
                                     :modal-content m/new-column-form
                                     :extras (-> local-state :field-idents :column/new-column-modal)
                                     :title "Create new column"}))
                       (= 1 (-> local-state :column/new-task-modal :state ))
                       (conj (modal {:root-query (get-root-query)
                                     :save-btn-state ( -> local-state :column/save-btn-field :state)
                                     :ref :column/new-task-modal
                                     :submit-fn (partial h/new-task-save)
                                     :modal-content m/new-task-form
                                     :extras (-> local-state :field-idents :column/new-task-modal)
                                     :title "Create new task"})))))))



(defui BoardItem
  static om/Ident
  (ident [_ item]
         [:board/by-id (:db/id item)])
  static om/IQuery
  (query [this]
         [:db/id :board/name :board/description])
  Object
  (render [this]         
          (let [{:keys [db/id board/name board/description]} (om/props this)]
            (dom/div 
             #js {:key #(str "board-item-div-" id)}
             (dom/a #js{:href (b/path-for router/router :columns :board-id id) :key #(str "board-item-div-a-" id)} name)
             (dom/p #js {:key #(str "board-item-div-p-" id)} description)))))

(def board-item (om/factory BoardItem {:keyfn :db/id}))


(defui BoardList
  static om/IQuery
  (query [this]
         `[{:board/list ~(om/get-query BoardItem)} {:app/local-state [:board/new-board-modal]} ])

  Object
  (render [this]
          (dom/div #js {:key "board-list"}
                     [(dom/div #js {:key "board-list-div1"} (apply
                                       dom/div #js {:key "board-list-div1"}
                                       (-> (map #(board-item %) (:board/list (om/props this)))
                                           vec)))
                      (dom/div #js {:key "board-list-div2"}
                               (dom/a #js {:href "#"
                                           :key "board-list-div2-a"
                                           :onClick #(h/modal-open {:reconciler (om/get-reconciler this) :ref :board/new-board-modal} )} "New board..."))
                      (let [{:keys [app/local-state]} (om/props this)]
                        (if (= 1 (-> local-state :board/new-board-modal :state))
                          (modal {:root-query (get-root-query)
                                  :save-btn-state (-> local-state :board/save-btn-field :state)
                                  :ref :board/new-board-modal
                                  :submit-fn (partial h/new-board-save)
                                  :modal-content m/new-board-form
                                  :title "Create new board"})))
                      ])))

(def route->component
  {:boards BoardList
   :columns ColumnList})

(def route->factory
  (zipmap (keys route->component)
          (map (fn [c] (om/factory c {:keyfn #(str (-> % keys first))})) (vals route->component))))

(defn get-root-query []
  {:route/data (zipmap (keys route->component)
                       (map om/get-query (vals route->component)))})

