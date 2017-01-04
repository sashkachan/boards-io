(ns boards-io.parser
  (:require 
   [om.next :as om]
   [goog.log :as glog]
   [boards-io.logger :as l]
   [om.next.impl.parser :as parser]))

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
    (cond-> { }
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

(defmethod read :default
  [{:keys [target state query parser db-path] :as env} k _]
  (let [st @state
        db-path' (conj db-path k)
        env' (assoc env :db-path db-path' )]
    (cond-> {}
      (and (not= nil target) (not= nil query))
      (merge (get-query-root env'))
      (nil? target)
      (assoc :value (merge (get-in st db-path') (parser env' query)))
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
        query' (get query route)]
    (if (nil? route)
      {:value nil}
      (let [parsed (parser env' [{route query'}] target)]
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
  (println "ident toggle-field " ident @state)
  (let [state' @state
        route (first (:app/route state'))]
    {:keys [:route/data :column/moving]
     :action (fn []
               (swap! state assoc-in [:route/data route :app/local-state field] {:state field-state})
               (swap! state assoc-in [:route/data route :app/local-state :field-idents] {field ident}))}))

; todo: include db-path (current route) in env
(defmethod mutate 'local/update-order!
  [{:keys [state]} _ {:keys [target-column]}]
  (let [st @state
        _ (println "state from update-order! "  )
        route (-> (get st :app/route) first)
        target-column-order (:column/order target-column)
        column-dragging? (= :drag-start (-> st :route/data :column/moving :state))
        dragged-column (-> st :route/data :field-idents :column/moving :column)
        dragged-column-order (:column/order dragged-column)
        columns (-> st :route/data route :column/list)]
    (cond-> {:keys [:column/list]}
      column-dragging?
      (assoc :action
             (fn []
               (let [diff (- dragged-column-order target-column-order)
                     order-k (cond
                               (> diff 0) 1
                               (< diff 0) -1
                               :else 0)
                     affected-orders-range (if (> order-k 0)
                                             (range target-column-order dragged-column-order)
                                             (range (+ 1 dragged-column-order) (+ 1 target-column-order)))
                     new-columns (map (fn [column]
                                        (let [order (:column/order column)
                                              new-order (cond
                                                          (= 0 order-k) order ;; not dragged over any other el
                                                          (= (:column/order column) dragged-column-order) ;; ->
                                                          target-column-order ;; 
                                                          (some #(= % order) affected-orders-range) (+ order order-k)
                                                          :else order)]
                                          (assoc column :column/order new-order))) columns)]
                 (swap! state assoc-in [:route/data route :column/list] new-columns)
                 (swap! state assoc-in [:route/data :field-idents :column/moving :column :column/order] target-column-order))
               )))))

(defmethod mutate 'local/loading!
  [{:keys [state]} _ {:keys [loading-state]}]
  (glog/info l/*logger* (str "mutating local/loading! to " loading-state))
  (let [st @state]
    {:keys [:app/local-state]
     :action (fn []
               (swap! state assoc :loading {:loading-state loading-state}))}))

(defmethod mutate :default
  [_  _ _]
  {:remote true})
