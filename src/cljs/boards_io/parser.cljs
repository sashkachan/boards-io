(ns boards-io.parser
  (:require 
   [om.next :as om]
   [goog.log :as glog]
   [boards-io.logger :as l]
   [om.next.impl.parser :as parser]))


(defn reorder-things [dragged-entity-id target-entity-id entity-order-key max-entity-order by-id]
  (let [dragged-entity-order (or (get-in by-id [dragged-entity-id entity-order-key]) (inc  max-entity-order))
        _ (println "dragged-entity-order " dragged-entity-order)
        target-entity-order (get-in by-id [target-entity-id entity-order-key])
        diff (- dragged-entity-order target-entity-order)
        order-k (cond
                  (> diff 0) 1
                  (< diff 0) -1
                  :else 0)
        affected-orders-range (if (> order-k 0)
                                (range target-entity-order (or dragged-entity-order max-entity-order) )
                                (range (+ 1 dragged-entity-order) (+ 1 target-entity-order)))
        new-by-id (into {}
                        (map (fn [[entity-id ent-map]]
                               (let [order (get ent-map entity-order-key) 
                                     new-order (cond
                                                 (= 0 order-k) order ;; not dragged over any other el
                                                 (= order dragged-entity-order) ;; ->
                                                 target-entity-order ;; 
                                                 (some #(= % order) affected-orders-range) (+ order order-k)
                                                 :else order)]
                                 {entity-id (assoc ent-map entity-order-key new-order)})) by-id))]
    new-by-id))

(defmulti update-order (fn [{:keys [dragged-column-id target-column-id dragged-task-id target-task-id] :as incoming}]
                         (let [dispatch (cond
                                          (and dragged-column-id target-column-id) :col-to-col
                                          (and dragged-task-id target-column-id) :task-to-col
                                          (and dragged-task-id target-task-id) :task-to-task
                                          :else :default)]
                           ;(println "dispatching " dispatch)
                           dispatch)))

(defmethod update-order :task-to-col
  [{:keys [dragged-task-id target-column-id state ]}]

  (let [current-shadow (get-in state [:task/by-id -1])
        current-shadow-column (get-in current-shadow [:task/column :db/id])
        dragged-task-order (or (:task/order current-shadow) (get-in state [:task/by-id dragged-task-id :task/order])) 
        _ (println "task-to-col " dragged-task-id target-column-id current-shadow-column)]
    (cond-> state
      true 
      (assoc-in [:task/by-id -1] {:db/id -1
                                  :task/column {:db/id target-column-id}
                                  :task/order dragged-task-order
                                  :task/name "------------"})
      (not= nil current-shadow-column)
      (update-in [:column/by-id current-shadow-column :task/_column]
                 (fn [c] (filterv #(not= [:task/by-id -1] %) c)))
      (not= nil target-column-id)
      (update-in [:column/by-id target-column-id :task/_column]
                 conj [:task/by-id -1])
      )))

(defmethod update-order :col-to-col
  [{:keys [dragged-column-id target-column-id column/by-id]}]
  (let [max-order (apply max (map (fn [[_ column]] (:column/order column)) by-id))]
    (reorder-things dragged-column-id target-column-id :column/order max-order by-id)))

(defmethod update-order :task-to-task
  [{:keys [dragged-task-id target-task-id state]}]

  (let [dragged-task-id-order (get-in state [:task/by-id dragged-task-id :task/order])
        source-column-id (get-in state [:task/by-id dragged-task-id :task/column :db/id])
        target-column-id (get-in state [:task/by-id target-task-id :task/column :db/id])
         _ (println "task-to-task " dragged-task-id target-task-id source-column-id target-column-id )
        current-shadow-column (get-in state [:task/by-id -1 :task/column :db/id])
        max-order (apply max
                         (map (fn [[_ task]] (:task/order task))
                              (filter (fn [[_ task]] (= target-column-id (get-in task [:task/column :db/id])))
                                      (get-in state [:task/by-id]))))
        _ (println "max order in col " max-order)
        reorder-state (assoc-in state [:task/by-id] (reorder-things -1 target-task-id :task/order max-order (:task/by-id state)))
        _ (println (:task/by-id reorder-state))]

    ;state'
    reorder-state
    ))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defn denorm-data-val [key {:keys [db-path state query]}]
  (let [state' @state
        refs (get-in state' db-path)]
    (when (not (empty? (get-in state' db-path)))
      (get (om/db->tree [{key query}] refs refs) key))))

(defmethod read :board/list [{:keys [ast target query state db-path] :as env} k _]
  (let [st @state]
    (cond-> {}
      (nil? target)
      (assoc :value (denorm-data-val k env))
      (not (nil? target))
      (assoc target (assoc ast :query-root true)))))

(defmethod read :column/list [{:keys [ast target route query state db-path] :as env} k params]
  (let [st @state]
    (cond-> {}
      (not= nil target)
      (assoc target (-> ast
                        (assoc :query-root true)
                        (assoc :params (second route))))
      (nil? target)
      (assoc :value (denorm-data-val k env))))
  )

(defn get-query-root
  [{:keys [ast target parser] :as env}]
  #_(println "get-query-root ast " ast)
  {target (update-in ast [:query]
                     #(let [q (if (vector? %) % [%])
                            res (parser env q target)]
                        res))})

(defn read-local-value [{:keys [target state query parser db-path] :as env}]
  (let [st @state
        parsed (parser env query)
        current (get-in st db-path)]
    (cond-> current
      (map? current) (merge parsed))))

(defmethod read :default
  [{:keys [target state query parser db-path] :as env} k _]
  (let [st @state
        db-path' (conj db-path k)
        env' (assoc env :db-path db-path' )
        ;_ (println (get-in st db-path'))
        ]
    (cond-> {}
      (and (not= nil target) (not= nil query))
      (merge (get-query-root env'))
      (nil? target)
      (assoc :value (read-local-value env'))
      (= k :app/local-state)
      (dissoc :remote))))

(defmethod read :route/data 
  [{:keys [target parser state query ast] :as env} k params]
  (let [st @state
        route (-> (get st :app/route) first)
        route-params (-> (get st :app/route) second)
        env' (-> env
                 (assoc :route [route route-params])
                 (assoc :db-path [:route/data])) 
        query' (if-not (empty? query)
                 (get (first query) route))]
    (if (nil? route)
      {:value nil}
      (let [parsed (parser env' [{route (or query' query)}] target)]
        (cond-> {}
          (not= nil target) (assoc target (parser/expr->ast (first parsed)))
          (= nil target ) (assoc :value parsed))))))

(defmethod read :app/route
  [{:keys [state query]} k _]
  (let [st @state]
     {:value (get st k)}))

(defmethod mutate 'local/route!
  [{:keys [state]} _ {:keys [route]}]
  {:keys [:route/data]
   :action (fn []
             (swap! state assoc :route/data nil)
             (swap! state assoc :app/route route))})

(defmethod mutate 'local/toggle-field!
  [{:keys [state]} _ {:keys [field field-state ident]}]
  (let [state' @state
        route (first (:app/route state'))]
    {:keys [:route/data]
     :action (fn []
               (swap! state assoc-in [:route/data route :app/local-state field] {:state field-state})
               (swap! state assoc-in [:route/data route :app/local-state :field-idents] {field ident}))}))

; todo: include db-path (current route) in env
(defmethod mutate 'local/update-order!
  [{:keys [state]} _ {:keys [target-column-id target-task-id]}]
  (let [st @state
        route (-> (get st :app/route) first)
        column-dragging? (= :drag-start (get-in st [:route/data route :app/local-state :column/moving :state]))
        task-dragging? (= :drag-start (get-in st [:route/data route :app/local-state :task/moving :state]))
        dragged-column-id (get-in st [:route/data route :app/local-state :field-idents :column/moving :column-id])
        dragged-task-id (get-in st [:route/data route :app/local-state :field-idents :task/moving :task-id]) ;
        dragged-task-column-id (get-in st [:route/data route :task/by-id dragged-task-id :task/column :db/id])
        target-task-column-id (get-in st [:route/data route :task/by-id target-task-id :task/column :db/id])
;        _ (println "task-column-id " target-task-column-id "target-column-id " target-column-id)
        columns (get-in st [:route/data route :column/by-id])
        tasks (get-in st [:route/data route :task/by-id])]
    {:action
     (fn []
       (println column-dragging? target-column-id task-dragging? target-task-id)
       (cond
         (and column-dragging? target-column-id (not= dragged-column-id target-column-id))
         (swap! state assoc-in [:route/data route :column/by-id]
                (update-order {:dragged-column-id dragged-column-id
                               :target-column-id target-column-id
                               :column/by-id columns}))
         (and task-dragging? target-column-id)
         (swap! state assoc-in [:route/data route]
                (update-order {:dragged-task-id dragged-task-id
                               :target-column-id target-column-id
                               :state (get-in st [:route/data route])}))
         (and task-dragging? target-task-id
              (not= dragged-task-id target-task-id)
              #_(= dragged-task-column-id target-task-column-id))
         (swap! state assoc-in [:route/data route]
                (update-order {:dragged-task-id dragged-task-id
                               :target-task-id target-task-id
                               :state (get-in st [:route/data route])}))))}))

(defmethod mutate 'local/loading!
  [{:keys [state]} _ {:keys [loading-state]}]
  (glog/info l/*logger* (str "mutating local/loading! " loading-state))
  (let [st @state]
    {:keys [:app/local-state]
     :action (fn []
               (swap! state assoc :loading {:loading-state loading-state}))}))

(defmethod mutate :default
  [_  _ _]
  {:remote true})

(def merger (fn [route->component]
              (fn [r s n q]
                {:keys [:route/data]    ; todo: dynamic keys 
                 :next (let [route (-> s :app/route first)
                             cmpn (get route->component route) 
                             cur-rd (get-in s [:route/data route])
                             new-rd (merge cur-rd (om/tree->db cmpn (get n route) true))]

                         (assoc-in s [:route/data route] new-rd))})))

                                        ; task to column
                                        ; task to task
                                        ; column to column
                                        ;


