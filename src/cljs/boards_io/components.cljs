(ns boards-io.components
  (:require [bidi.bidi :as b]
            [boards-io.handlers :as h]
            [boards-io.router :as router]
            [boards-io.modals :as m]
            [boards-io.drawcanvas :as cnv]
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
             #js {:className "board-item" }
             (dom/a #js{:href (b/path-for router/router :columns :board-id id)} name)
             (dom/p nil description)))))

(def board-item (om/factory BoardItem {:keyfn :db/id}))


(defui BoardList
  static om/IQuery
  (query [this]
         `[{:board/list ~(om/get-query BoardItem)} {:app/local-state [:board/new-board-modal]}])
  
  Object
  (render [this]
          (dom/div #js {:className "board-list"}
                   [(dom/div #js {:key "board-list-div1"}
                             (apply
                              dom/div #js {:key "board-list-div1"}
                              (-> (map #(board-item %) (:board/list (om/props this)))
                                  vec)))
                    (dom/div #js {:key "board-list-div2"}
                             (dom/a #js {:href "#"
                                         :key "board-list-div2-a"
                                         :onClick #(h/modal-open {:reconciler (om/get-reconciler this) :ref :board/new-board-modal} )} "New board..."))
                    (let [{:keys [app/local-state]} (om/props this)]
                      (modal {:root-query (get-root-query)
                              :show (= 1 (-> local-state :board/new-board-modal :state))
                              :save-btn-state (-> local-state :board/save-btn-field :state)
                              :ref :board/new-board-modal
                              :submit-fn (partial h/new-board-save)
                              :modal-content m/new-board-form
                              :title "Create new board"}))])))

(defui ColumnTask
  static om/Ident
  (ident [_ item]
         [:task/by-id (:db/id item)])
  static om/IQuery
  (query [this]
         [:db/id :task/name :task/order])
  Object
  (componentWillUnmount [this])
  
  (render [this]
          (let [is-moving? (:moving (om/props this))
                task (om/props this)
                task-item-m (clj->js
                             (cond-> {:className "board-column-task-item"
                                      :draggable "true"
                                      :key (:db/id task)
                                      :style #js {:order (:task/order task) }
                                      :onMouseEnter (fn [e]
                                                      #_(h/mouse-enter {:reconciler (om/get-reconciler this)
                                                                        :entity :task/over
                                                                        :entity-id (:db/id task)}))
                                      :onMouseLeave (fn [e]
                                                      #_(h/mouse-leave {:reconciler (om/get-reconciler this)
                                                                        :entity :task/over
                                                                        :entity-id (:db/id task)}))
                                      :onDrop (fn [e]
                                                (h/drag-end-task {:reconciler (om/get-reconciler this)}))
                                      :onDragStart (fn [e]
                                                     (let [ ;e (.-nativeEvent e)
                                                           _ (.setData (.-dataTransfer e) "text/plain" "")
                                        ;_ (.setDragImage (.-dataTransfer e) (cnv/get-image) 10 10)
                                                           _ (aset (.-dataTransfer e) "dropEffect" "move")
                                                           _ (aset (.-dataTransfer e) "effectAllowed" "move")
                                                           drag-data-map {:component this
                                                                          :reconciler (om/get-reconciler this)
                                                                          :entity :task/moving
                                                                          :ident {:task-id (-> (:db/id task))}}]
                                                       #_(aset (.-target e) "style" "transform: rotate(2deg);")
                                                       (h/drag-start drag-data-map)
                                                       (.stopPropagation e)))
                                      :onDragOver (fn [e]
                                                    (let [height (-> e (.-nativeEvent ) (.-target) (.-clientHeight ))
                                                          offset (-> e (.-nativeEvent ) (.-offsetY))
                                                          dragbuf (/ height 10)
                                                          dir (if (> (- height offset) dragbuf) :top :bottom)]
                                                      (h/update-order {:reconciler (om/get-reconciler this)
                                                                       :component this
                                                                       :entity :target-task-id
                                                                       :extra {:direction dir}
                                                                       :entity-id (:db/id task)})
                                                      (.stopPropagation e)
                                                      (.preventDefault e)))}
                               is-moving? (update :className #(str % " moving"))))]
            (if (not= nil (:task/order task))
              (dom/div task-item-m (:task/name (om/props this)))))))

(def column-task (om/factory ColumnTask {:keyfn :db/id}))

(defui ColumnItem
  static om/Ident
  (ident [_ item]
         [:column/by-id (-> item :db/id)])
  
  static om/IQuery
  (query [this]
         `[:db/id :column/name :column/order {:column/board ~(om/get-query BoardItem)}
           {:column/tasks ~(om/get-query ColumnTask)}])
  

  Object
  (render [this]
          (let [is-moving? (-> this om/props :moving)
                task-over (-> this om/props :task/over)
                mov-task-id (:task/moving (om/props this))
                class-name (str "board-column " (if is-moving? "moving" ""))
                style #js {:order (-> this om/props :column/order) }
                column-id (:db/id (om/props this))
                drag-data-map {:component this
                               :reconciler (om/get-reconciler this)
                               :entity :column/moving
                               :ident {:column-id column-id}}
                column-tasks (mapv #(cond-> %
                                      (and (= (:db/id %) mov-task-id))
                                      (assoc :moving true)) (:column/tasks (om/props this)))
                js-map (cond-> {:className class-name
                                :key (str "item-" column-id)
                                :style style
                                :draggable "true"
                                :onDragStart (fn [e]
                                               (let [e (.-nativeEvent e)
                                                     _ (.setData (.-dataTransfer e) "text/plain" "")
                                        ;_ (.setDragImage (.-dataTransfer e) (cnv/get-image) 10 10)
                                                     _ (aset (.-dataTransfer e) "dropEffect" "move")
;                                                     _ (aset (.-dataTransfer e) "effectAllowed" "move")
                                                     ]
                                                 (h/drag-start drag-data-map)))
                                :onDrop (fn [e] )
                                :onDragOver (fn [e]
                                              #_(.stopPropagation e)
                                              (.preventDefault e))
                                :onDragEnd (fn [e] 
                                             (.preventDefault e)
                                             (h/drag-end-task {:reconciler (om/get-reconciler this)})
                                             (h/drag-end-column drag-data-map))}
                         (not is-moving?)
                         (assoc :onDragEnter
                                (fn [e]
                                  (h/update-order {:reconciler (om/get-reconciler this) :component this :entity :target-column-id :entity-id column-id})
                                  (.preventDefault e))))]
            (dom/div (clj->js js-map) 
                     [(dom/div #js {:className "board-column-title" :key (str "item-title-" column-id)} (str (:column/name (om/props this))))
                      (dom/div #js {:className "board-column-tasks" :key (str "board-column-tasks-" column-id)}
                               (mapv column-task column-tasks))
                      (dom/div #js {:className "board-column-new-item" :key (str "new-item-div-" column-id)}
                               (dom/a #js {:href "#" :onClick #(h/modal-open {:reconciler (om/get-reconciler this) :ref :column/new-task-modal :ident {:column-id (:db/id (om/props this))}} )} "New item..." ))]))))

(def column-item (om/factory ColumnItem {:keyfn :db/id}))

(defui ColumnList
  static om/IQuery
  (query [this]
         `[{:column/list ~(om/get-query ColumnItem)}
           {:app/local-state [:column/moving
                              :column/new-column-modal
                              :column/new-task-modal
                              :column/save-btn-field
                              :task/moving
                              {:field-idents [:column/moving :task/moving ]}]}
           :app/route])

  Object
  (render
   [this]
   (let [{:keys [app/local-state]} (om/props this)
         board-id (js/parseInt (-> (om/props this) :app/route second :board-id))
         proc-col-item (fn [col]
                         (let [mov-col-id (-> local-state :field-idents :column/moving :column-id)
                               mov-task-id (-> local-state :field-idents :task/moving :task-id)
                               column-moving (-> local-state :column/moving :state)
                               task-moving (-> local-state :task/moving :state)
                               col (cond-> col
                                     (and (= column-moving :drag-start) (= (:db/id col) mov-col-id))
                                     (assoc :moving true)
                                     (= :drag-start task-moving) (assoc :task/moving mov-task-id))]
                           (column-item col)))]
     (dom/div #js {:className "board-wrap" :key (str "board-wrap-" board-id)}
              (merge
               (into [] (map proc-col-item (:column/list (om/props this))))
               (dom/div #js {:className "board-column new-column" :key "board-column-new-column"}
                        (dom/a #js {:href "#"
                                    :key "board-column-new-column-href"
                                    :onClick #(h/modal-open
                                               {:reconciler (om/get-reconciler this)
                                                :ref :column/new-column-modal
                                                :ident {:board-id board-id}} )} "New column..."))
               (modal {:root-query (get-root-query)
                       :show (= 1 (-> local-state :column/new-column-modal :state))
                       :save-btn-state (-> local-state :column/save-btn-field :state)
                       :ref :column/new-column-modal
                       :submit-fn (partial h/new-column-save)
                       :modal-content m/new-column-form
                       :extras (-> local-state :field-idents :column/new-column-modal)
                       :title "Create new column"})
               (modal {:root-query (get-root-query)
                       :show (= 1 (-> local-state :column/new-task-modal :state ))
                       :save-btn-state ( -> local-state :column/save-btn-field :state)
                       :ref :column/new-task-modal
                       :submit-fn (partial h/new-task-save)
                       :modal-content m/new-task-form
                       :extras (-> local-state :field-idents :column/new-task-modal)
                       :title "Create new task"}))))))

(defui AuthHeader
  static om/IQuery
  (query [this]
         [{:oauth/user [:user/email :user/userid :user/token]}])

  Object
  (render [this]
          (let [{:keys [user/email user/userid user/token] :as lp} (get (om/props this) :oauth/user)]
            (if token
              (dom/div nil email)
              (dom/div nil
                       (dom/a #js {:href "/oauth" :target "_self"} "Login with Google"))))))


(defui Header
  Object
  (render
   [this]
   (dom/nav #js {:className "navbar navbar-default" :id "header"}
            (dom/div #js {:className "container-fluid"}
                     [(dom/div #js {:className "col-md-10" :key "nav-col-10"}
                               [(dom/div #js {:className "navbar-header" :key "navbar-head"}
                                         (dom/a #js {:className "navbar-brand" :href "/"} "Boards.io"))
                                (dom/div #js {:className  "collapse navbar-collapse" :key "navbar-collapse"}
                                                                    (dom/ul #js {:className "nav navbar-nav"}
                                                                            (dom/li #js {:id "boards-list"} 
                                                                                    (dom/a #js {:href "#"} "Boards"))))])
                      (dom/div #js {:className "col-md-2" :key "col-md-2"} 
                               ((om/factory AuthHeader {:keyfn identity}) (om/props this) ))]))))

(def route->component
  {:columns ColumnList
   :boards  BoardList
   :auth BoardList})

(def route->factory
  (zipmap (keys route->component)
          (map (fn [c] (om/factory c {:keyfn #(str (-> % keys first))})) (vals route->component))))


(defn get-root-query []  
  {:route/data (mapv hash-map (keys route->component)
                     (map om/get-query (vals route->component)))})
