(ns boards-io.components
  (:require [bidi.bidi :as b]
            [boards-io.handlers :as h]
            [boards-io.router :as router]
            [boards-io.modals :as m]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui ui]]))

(declare get-root-query)

(def modal (om/factory m/Modal))

(defui ColumnTasks
  Object
  (render [this]
          (apply dom/div #js {:className "board-column-tasks"}
                 (vec (map #(dom/div #js {:className "board-column-task-item"} (:task/name %)) (om/props this))))))

(def column-tasks (om/factory ColumnTasks))

(defui ColumnItem
  Object
  (render [this]
          #_(println "ColumnItem" ( om/props this))
          (let [class-name (str "board-column " (if (-> this om/props :moving) "moving" ""))
                style #js { :order (-> this om/props :column/order) }]
            (dom/div #js {:className class-name
                          :style style
                          :onMouseDown (fn [e]
                                         (h/moving-init
                                          {:reconciler this
                                           :root-query (get-root-query)
                                           :component this
                                           :ident {:column-id (:db/id (om/props this))}}))}
                     [(dom/div #js {:className "board-column-title"} (str (:column/name (om/props this)) ))
                      (column-tasks (:task/_column (om/props this)))
                      (dom/div #js {:className "board-column-new-item"}
                               (dom/a #js {:href "#" :onClick #(h/modal-open {:reconciler (om/get-reconciler this) :ref :column/new-task-modal :ident {:column-id (:db/id (om/props this))}} )} "New item..." ))]))))

(def column-item (om/factory ColumnItem))

#_(defui ColumnWrap
  Object
  (render [this]
          (let [{:keys [moving field-idents] :as props} (om/props this)]
            (println "ColumnWrap list " props)
            (apply dom/div nil
                   (map (fn [e]
                          (let [mov-col-id (-> field-idents :column-id) ]
                            (println "mov-col-id " mov-col-id " col-id " (:db/id e) )
                            (if (= (:db/id e) mov-col-id)
                              (column-item (assoc e :moving true))
                              (column-item e)))) (:list props)))
            )))

#_(def column-wrap (om/factory ColumnWrap))

(defn column-wrap [props]
  (let [{:keys [moving field-idents]} props]
    (vec (map (fn [e]
                (let [mov-col-id (-> field-idents :column-id)]
                  (if (= (:db/id e) mov-col-id)
                    (column-item (assoc e :moving true))
                    (column-item e)))) (:list props)))))

(defui ColumnList
  static om/Ident
  (ident [_ item]
         (println "ColList Ident " item)
         [:column/by-id (-> item :db/id)])

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
                      (column-wrap {:list (:column/list (om/props this))
                                    :moving (-> local-state :column/moving )
                                    :field-idents (-> local-state :field-idents :column/moving) })
                                    
                                    (dom/div #js {:className "board-column new-column"}
                                             (dom/a #js {:href "#"
                                                         :onClick #(h/modal-open
                                                                    {:reconciler (om/get-reconciler this)
                                                                     :ref :column/new-column-modal
                                                                     :ident {:board-id board-id}} )} "New column...")))]
            (println "ColumnList render props: " (om/props this))
            (dom/div #js {:className "board-wrap"}
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
                                     :title "Create new task"}) ))))))

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
             nil
             (dom/a #js{:href (b/path-for router/router :columns :board-id id)} name)
             (dom/p nil description)))))

(def board-item (om/factory BoardItem {:keyfn :db/id}))


(defui BoardList
  static om/IQuery
  (query [this]
         `[{:board/list ~(om/get-query BoardItem)} {:app/local-state [:board/new-board-modal]} ])

  Object
  (render [this]
          (println "BoardList props: " (om/props this))
          (dom/div nil
                     [(dom/div nil (apply
                                    dom/div nil
                                    (-> (map #(board-item %) (:board/list (om/props this)))
                                        vec)))
                      (dom/div nil
                               (dom/a #js {:href "#"
                                           :onClick #(h/modal-open {:reconciler (om/get-reconciler this) :ref :board/new-board-modal} )} "New board..."))
                      (let [{:keys [app/local-state]} (om/props this)]
                        (println "app/local-state is " local-state)
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
          (map om/factory (vals route->component))))

(defn get-root-query []
  {:route/data (zipmap (keys route->component)
                       (map om/get-query (vals route->component)))})

