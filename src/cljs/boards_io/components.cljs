(ns boards-io.components
  (:require [bidi.bidi :as b]
            [boards-io.handlers :as h]
            [boards-io.router :as router]
            [boards-io.modals :as m]
            [om.dom :as dom]
            [goog.events :as events]
            [om.next :as om :refer-macros [defui ui]]
            [goog.log :as glog]
            [boards-io.logger :as l]))

(declare get-root-query ColumnItem)

(def modal (om/factory m/Modal {:keyfn :ref}))

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
             #js {:key (str "board-item-div-" id)}
             (dom/a #js{:href (b/path-for router/router :columns :board-id id) :key (str "board-item-div-a-" id)} name)
             (dom/p #js {:key (str "board-item-div-p-" id)} description)))))

(def board-item (om/factory BoardItem {:keyfn :db/id}))


(defui BoardList
  static om/IQuery
  (query [this]
         `[{:board/list ~(om/get-query BoardItem)} {:app/local-state [:board/new-board-modal]}])

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
                                  :title "Create new board"})))])))

(defui ColumnTask
  static om/Ident
  (ident [_ item]
         [:task/by-id (:db/id item)])
  static om/IQuery
  (query [this]
         [:db/id :task/column :task/name :task/order])
  Object
  (render [this]
          (let [is-moving? (:moving (om/props this))
                task (om/props this)
                task-item-m (clj->js
                             (cond-> {:className "board-column-task-item"
                                      :draggable "true"
                                      :key (:db/id task)
                                      :style #js {:order (-> this om/props :task/order) }
                                      :onDragEnd (fn [e]
                                                   (glog/info l/*logger* "onDragEnd task")
                                                   (h/drag-end-task {:reconciler (om/get-reconciler this)
                                                                     :ident {:task-id (:db/id (om/props this))}})
                                                   (.stopPropagation e))
                                      :onDragEnter (fn [e]
                                                     ;(println "drag-enter-task " (:db/id task))
                                                     (h/update-order {:reconciler (om/get-reconciler this)
                                                                      :component this
                                                                      :entity :target-task-id
                                                                      :entity-id (:db/id task)})
                                                     (.stopPropagation e))
                                      :onDragStart (fn [e]
                                                     (glog/info l/*logger* "onDragStart task")
                                                     (h/drag-start {:reconciler (om/get-reconciler this)
                                                                    :component this
                                                                    :entity :task/moving
                                                                    :ident {:task-id (:db/id task)}})
                                                     (.stopPropagation e)
                                                     )}
                               is-moving? (update :className #(str % " moving"))))]
            (dom/div task-item-m (:task/name (om/props this))))))

(def column-task (om/factory ColumnTask {:keyfn :db/id}))

(defui ColumnItem
  static om/IQuery
  (query [this]
         [:db/id :column/name :column/order {:column/board (om/get-query BoardItem)}
          {:task/_column (om/get-query ColumnTask)}])
  
  static om/Ident
  (ident [_ item]
         [:column/by-id (-> item :db/id)])
  Object
  (render [this]
          (let [local-state (:app/local-state (om/props this))
                is-moving? (-> this om/props :moving)
                class-name (str "board-column " (if is-moving? "moving" ""))
                style #js {:order (-> this om/props :column/order) }
                column-id (:db/id (om/props this))
                moving-task-id (when (= :drag-start (-> local-state :task/moving :state))
                                 (-> this om/props :field-idents :task/moving :task-id))
                drag-data-map {:component this
                               :reconciler (om/get-reconciler this)
                               :entity :column/moving
                               :ident {:column-id column-id}}
                js-map (cond-> {:className class-name
                                :key (str "item-" column-id)
                                :style style
                                :draggable "true"
                                :onDragStart (fn [e] (h/drag-start drag-data-map))
                                :onDragEnd (fn [e] (h/drag-end-column drag-data-map))}
                        (not is-moving?)
                        (assoc :onDragEnter
                               (fn [e]
                                 (h/update-order {:reconciler (om/get-reconciler this) :component this :entity :target-column-id :entity-id column-id}))))]
            (dom/div (clj->js js-map) 
                     [(dom/div #js {:className "board-column-title" :key (str "item-title-" column-id)} (str (:column/name (om/props this))))
                      (dom/div #js {:className "board-column-tasks" :key (str "board-column-tasks-" column-id)}
                               (vec
                                (map
                                 #(column-task (if (= moving-task-id (:db/id %))
                                                 (assoc % :moving true)
                                                 %))
                                 (:task/_column (om/props this)))))
                      (dom/div #js {:className "board-column-new-item" :key (str "new-item-div-" column-id)}
                               (dom/a #js {:href "#" :onClick #(h/modal-open {:reconciler (om/get-reconciler this) :ref :column/new-task-modal :ident {:column-id (:db/id (om/props this))}} )} "New item..." ))]))))

(def column-item (om/factory ColumnItem {:keyfn :db/id}))

(defui ColumnList
  static om/IQueryParams
  (params [this]
          {:board-id 0})
  static om/IQuery
  (query [this]
         `[({:column/list ~(om/get-query ColumnItem)} {:board-id ?board-id})
           {:app/local-state [:column/moving
                              :column/new-column-modal
                              :column/new-task-modal
                              :column/save-btn-field
                              {:field-idents [:column/moving :task/moving]}]}
           :app/route])

  Object
  (render
   [this]
   (let [{:keys [app/local-state]} (om/props this)
         board-id (js/parseInt (-> (om/props this) :app/route second :board-id))
         proc-col-item (fn [item]
                         (let [mov-col-id (-> local-state :field-idents :column/moving :column-id)
                               mov-task-id (-> local-state :field-idents :task/moving :task-id)
                               column-moving (-> local-state :column/moving :state)
                               task-moving (-> local-state :task/moving :state)
                               task-items (into []
                                                (map #(cond-> %
                                                        (and (= (:db/id %) mov-task-id)
                                                             (= :drag-start task-moving))
                                                        (assoc :moving true)) (:task/_column item)))
                               item (cond-> item
                                      (and (= column-moving :drag-start) (= (:db/id item) mov-col-id))
                                      (assoc :moving true))
                               item (assoc item :task/_column task-items)]
                           (column-item item)))
         cols (into [] (map proc-col-item (:column/list (om/props this))))
         cols (merge
               cols
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





(def route->component
  {:boards BoardList
   :columns ColumnList})

(def route->factory
  (zipmap (keys route->component)
          (map (fn [c] (om/factory c {:keyfn #(str (-> % keys first))})) (vals route->component))))

(defn get-root-query []
  {:route/data [(zipmap (keys route->component)
                       (map om/get-query (vals route->component)))]})
