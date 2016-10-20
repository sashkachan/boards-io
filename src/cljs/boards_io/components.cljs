(ns boards-io.components
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [bidi.bidi :as b]
            [boards-io.router :as router]))



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
         '[({:column/list [ :column/name {:column/board [:db/id :board/name :board/description]}]} {:board-id ?board-id}) ]
 
         )

  Object
  (render [this]
          (println "ColumnList render props: " (om/props this))
          (apply dom/div nil
                 (vec (map
                      (fn [c]
                        (let [{:keys [column/name column/description column/board]} c]
                          (dom/div nil (str name " - " (:board/name board)))))
                      (:column/list (om/props this)))
                     ))))

(defui BoardItem
  static om/Ident
  (ident [_ item]
         [:board/by-id (:db/id item)])
  static om/IQuery
  (query [this]
         [:db/id :board/name :board/description])
  Object
  (render [this]
          (println "BoardItem props: " (om/props this))
          (let [{:keys [db/id board/name board/description]} (om/props this)]
            (dom/div 
             #js {:className }
             (dom/a #js{:href (b/path-for router/router :columns :board-id id)} name)
             (dom/p nil description)))))

(def board-item (om/factory BoardItem))

(defui BoardList
  static om/IQuery
  (query [this]
         [{:board/list (om/get-query BoardItem)}])
  static Object
  (render [this]
          (println "BoardList props:: " (om/props this))
          (dom/div nil
                   (apply
                    dom/div #js { :className "row"}
                    (-> (map #(board-item %) (:board/list (om/props this)))
                        vec)))))
