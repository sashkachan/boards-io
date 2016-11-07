(ns boards-io.components
  (:require [bidi.bidi :as b]
            [boards-io.handlers :as h]
            [boards-io.router :as router]
            [boards-io.modals :as m]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui ui]]))

(declare get-root-query)

(defui ColumnTasks
  Object
  (render [this]
          (apply dom/div #js {:className "board-column-tasks"}
                 (vec (map #(dom/div #js {:className "board-column-task-item"} (:task/name %)) (om/props this))))))

(def column-tasks (om/factory ColumnTasks))

(defui ColumnItem
  Object
  (render [this]
          (println "ColumnItem")
          (dom/div #js {:className "board-column "}
                   [(dom/div #js {:className "board-column-title"} (str (:column/name (om/props this))))
                    (column-tasks (:task/_column (om/props this)))] )))

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
         '[({:column/list [:db/id :column/name {:column/board [*]} {:task/_column [*]}]} {:board-id ?board-id}) ])

  Object
  (render [this]
          (println "ColumnList render props: " (om/props this))
          (apply dom/div #js {:className "board-wrap"}
                 (vec (map
                       (om/factory ColumnItem)
                       (:column/list (om/props this)))))))

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
(def new-board-item (om/factory m/NewBoardItemModal))


(defui BoardList
  static om/IQuery
  (query [this]
         `[{:board/list ~(om/get-query BoardItem)} :app/local-state])

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
                                           :onClick #(h/new-board {:reconciler (om/get-reconciler this)} )} "New board..."))
                      (let [{:keys [app/local-state]} (om/props this)]
                        (if (= 1 (:board/new-board-modal local-state))
                          (new-board-item {:root-query (get-root-query)
                                           :save-btn-state (:board/save-btn-field local-state)})))
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

