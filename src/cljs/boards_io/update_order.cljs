(ns boards-io.update-order)

(defn coltasks->map [st]
  (map (fn [[cid col]]
         (assoc col :column/tasks
                (into {}
                      (map (fn [[_ tid]] [tid cid]) (:column/tasks col)))))
       (get-in st [:column/by-id])))

(defn taskid->columnid [st task-id]
  (some (fn [c] (some-> c :column/tasks (get task-id))) (coltasks->map st)))

(defn reorder-columns [dragged-entity-id target-entity-id entity-order-key max-entity-order by-id]
  (let [dragged-entity-order (get-in by-id [dragged-entity-id entity-order-key])
        target-entity-order (get-in by-id [target-entity-id entity-order-key])
        diff (- dragged-entity-order target-entity-order)
        order-k (cond
                  (> diff 0) 1
                  (< diff 0) -1
                  :else 0)
        affected-orders-range (if (> order-k 0)
                                (range target-entity-order (or dragged-entity-order max-entity-order) )
                                (range (+ 1 dragged-entity-order) (+ 1 target-entity-order)))]

    (into {}
          (map (fn [[entity-id ent-map]]
                 (let [order (get ent-map entity-order-key) 
                       new-order (cond
                                   (= 0 order-k) order ;; not dragged over any other el
                                   (= order dragged-entity-order) ;; ->
                                   target-entity-order            ;; 
                                   (some #(= % order) affected-orders-range) (+ order order-k)
                                   :else order)]
                   {entity-id (assoc ent-map entity-order-key new-order)})) by-id))))

(defn reorder-tasks [dragged-entity-id target-entity-id entity-order-key by-id direction]
  (let [dragged-entity-order (get-in by-id [dragged-entity-id entity-order-key])
        target-entity-order (get-in by-id [target-entity-id entity-order-key])
        target-entity-column-id (get-in by-id [target-entity-id :task/column :db/id])
        by-id' (if (= nil dragged-entity-order)
                 by-id
                 (into {}
                       (map (fn [[entity-id ent-map]]
                              (let [order (get ent-map entity-order-key)
                                    entity-column-id (-> ent-map :task/column :db/id)
                                    new-order (cond
                                                (= entity-id dragged-entity-id)
                                                nil
                                                (and (= entity-column-id target-entity-column-id) (> order dragged-entity-order))
                                                (dec order)
                                                :else order)]
                                (if (= entity-column-id target-entity-column-id)
                                  {entity-id (assoc ent-map entity-order-key new-order)}
                                  {entity-id ent-map}))) by-id)))]

    (into {}
          (map (fn [[entity-id ent-map]]
                 (let [order (get ent-map entity-order-key)
                       entity-column-id (-> ent-map :task/column :db/id)
                       dragged-entity-new-order (if (= :bottom direction)
                                                  (inc target-entity-order)
                                                  target-entity-order)
                       new-order (cond
                                   (= entity-id dragged-entity-id)
                                   dragged-entity-new-order
                                   (and (= entity-column-id target-entity-column-id) (>= order dragged-entity-new-order))
                                   (inc order)
                                   :else order)]
                   (if (= entity-column-id target-entity-column-id )
                     {entity-id (assoc ent-map entity-order-key new-order)}
                     {entity-id ent-map}))) by-id'))
    

    ))

(defmulti update-order! (fn [s {:keys [dragged-column-id target-column-id target-task-id] :as incoming}]
                         (let [dispatch (cond
                                          (and dragged-column-id target-column-id) :col-to-col
                                          (and (not dragged-column-id) target-column-id) :task-to-col
                                          target-task-id :task-to-task
                                          :else :default)]
                           dispatch)))

(defmethod update-order! :col-to-col
  [state {:keys [dragged-column-id target-column-id]}]
  (let [st @state
        by-id (get-in st [:column/by-id])]
    (let [max-order (apply max (map (fn [[_ column]] (:column/order column)) by-id))]
      (swap! state assoc-in
             [:column/by-id]
             (reorder-columns dragged-column-id target-column-id :column/order max-order by-id)))))

(defmethod update-order! :task-to-col
  [state {:keys [dragged-task-id target-column-id]}]
  (let [state' @state
        current-shadow (get-in state' [:task/by-id dragged-task-id])
        current-shadow-column (taskid->columnid state' dragged-task-id)
        dragged-task-order (:task/order current-shadow)
        dragged-task (get-in state' [:task/by-id dragged-task-id])
        target-col-tasks  (get-in state' [:column/by-id target-column-id :column/tasks])
        new-shadow current-shadow #_(-> dragged-task
                       (assoc-in [:task/column :db/id] target-column-id))]
    (when
      (or (= target-column-id current-shadow-column)
          (= nil current-shadow-column))
      (swap! state assoc-in [:task/by-id dragged-task-id] new-shadow) )
    (when (and (not= nil current-shadow-column)
               (not= target-column-id current-shadow-column))
      (swap! state assoc-in [:task/by-id dragged-task-id]
             (assoc-in new-shadow [:task/order]
                       (if (not= 0 (count target-col-tasks))
                         nil
                         1))))
    
    (when (not= nil current-shadow-column)
      (swap! state update-in [:column/by-id current-shadow-column :column/tasks]
             (fn [c] (filterv #(not= [:task/by-id dragged-task-id] %) c))))
    

    (when (not= nil target-column-id)
      (swap! state update-in [:column/by-id target-column-id :column/tasks]
             conj [:task/by-id dragged-task-id]))))

(defmethod update-order! :task-to-task
  [state {:keys [dragged-task-id target-task-id direction]}]
  (let [state' @state
        current-shadow-column (taskid->columnid state' dragged-task-id)
        current-shadow-column-order (get-in state [:task/by-id dragged-task-id :task/order]) ]
    (cond
      (not= dragged-task-id target-task-id)
      (swap! state
             assoc-in [:task/by-id]
             (reorder-tasks dragged-task-id target-task-id :task/order (:task/by-id @state) direction)))))


