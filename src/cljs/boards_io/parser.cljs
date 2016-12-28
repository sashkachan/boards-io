(ns boards-io.parser
  (:require 
   [om.next :as om]
   [goog.log :as glog]
   [om.next.impl.parser :as parser]))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :board/list [{:keys [ast target] :as env} _ _]
  #_(println ":board/list ast " ast)
  {target (assoc ast :query-root true)})

(defmethod read :column/list [{:keys [ast target route query] :as env} _ params]
  {target (-> ast
              (assoc :query-root true)
              (assoc :params (second route)))})

(defn get-query-root
  [{:keys [ast target parser] :as env}]
  #_(println "get-query-root ast " ast)
  {target (update-in ast [:query]
                     #(let [q (if (vector? %) % [%])
                            res (parser env q target)]
                        res))})

(defmethod read :default
  [{:keys [target state query parser db-path] :as env} k _]
  #_(println k " query " query " subquery: " (parser env query target) " target " target)
  (let [st @state
        path (conj db-path k)]
    (cond-> {}
      (and (not= nil target) (not= nil query))
      (merge (get-query-root env))
      (nil? target)
      (assoc :value (merge (get-in st path) (parser env query))))))


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
        #_(println "parsed: " parsed)
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
  {:keys [:app/local-state]
   :action (fn []
             (swap! state assoc-in [:route/data field] {:state field-state} )
             (swap! state assoc-in [:route/data :field-idents] {field ident}))})

(defmethod mutate 'local/update-order!
  [{:keys [state]} _ {:keys [target-column]}]
  (let [st @state
        route (-> (get st :app/route) first)
        target-column-order (:column/order target-column)
        dragged-column (-> st :route/data :field-idents :column/moving :column)
        dragged-column-order (:column/order dragged-column)
        columns (-> st :route/data :columns :column/list)]
    {:keys [:column/list]
     :action
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
                                                  (= 0 order-k) order
                                                  (= (:column/order column) dragged-column-order) ;; ->
                                                  target-column-order
                                                  (some #(= % order) affected-orders-range) (+ order order-k)
                                                  :else order)]
                                  (assoc column :column/order new-order))) columns)]
         (swap! state assoc-in [:route/data :columns :column/list] new-columns)
         (swap! state assoc-in [:route/data :field-idents :column/moving :column :column/order] target-column-order))
       )}))

(defmethod mutate :default
  [_  _ _]
  {:remote true})
