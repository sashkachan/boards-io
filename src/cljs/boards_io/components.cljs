(ns boards-io.components
  (:require [bidi.bidi :as b]
            [boards-io.handlers :as h]
            [boards-io.router :as router]
            [om.dom :as dom]
            [om.next :as om :refer-macros [defui ui]]))

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
         `[({:column/list [ :column/name {:column/board [:db/id :board/name :board/description]}]} {:board-id ?board-id}) ])

  Object
  (render [this]
          (println "ColumnList render props: " (om/props this))
          (apply dom/div nil
                 (vec (map
                      (fn [c]
                        (let [{:keys [column/name column/description column/board]} c]
                          (dom/div nil (str name " - " (:board/name board)))))
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

(defui NewBoardItemModal

  Object
  (render [this]
          (dom/div nil (dom/div nil "new-board-item render"))))

(def board-item (om/factory BoardItem {:keyfn :db/id}))
(def new-board-item (om/factory NewBoardItemModal))


(defui BoardList
  static om/IQuery
  (query [this]
         `[{:app/local-state [:board/new-board-modal]} {:board/list ~(om/get-query BoardItem)}])
  
  Object
  (render [this]
          (println "BoardList props: " (om/props this))
          (dom/div nil
                     [(dom/div nil (apply
                                    dom/div nil
                                    (-> (map #(board-item %) (:board/list (om/props this)))
                                        vec)))
                       (dom/div nil (dom/a #js {:href "#"
                                                  #_:onClick 
                                                  } "New board...") )
                      (let [{:keys [app/local-state] } (om/props this)]
                        (if (= 1 (:board/new-board-modal local-state))
                          (new-board-item)))
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

(defui Root
  static om/IQuery
  (query [this]
         `[:app/route
          ~(get-root-query)])
  
  Object
  (render [this]
          (println "root data " (om/props this))
          (let [{:keys [app/route route/data]} (om/props this)
                pr (first route)]
            (if (not= nil pr)
              ((route->factory pr) (get data pr)))
            )))

