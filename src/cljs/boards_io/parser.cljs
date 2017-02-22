(ns boards-io.parser
  (:require 
   [om.next :as om]
   [goog.log :as glog]
   [boards-io.logger :as l]
   [boards-io.update-order :as uo]
   [om.next.impl.parser :as parser])
  (:import [goog.net Cookies]))

(def cookies (Cookies. js/document))
(defn get-cookie [name] (.get cookies name))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defn denorm-data-val [key {:keys [db-path ast state query]}]
  (let [state' @state
        refs (get-in state' db-path)
        q [(om/ast->query ast)]]
    (when (not (empty? refs))
      (get (om/db->tree q refs state') key))))

(defmethod read :oauth/user [{:keys [ast target route query state db-path] :as env} k params]
  (let [st @state]
    (cond-> {}
      (nil? target)
      (assoc :value (denorm-data-val k env))
      (not= nil target)
      (assoc target (-> ast
                        (assoc :query-root true)
                        (assoc :params {:token (get-cookie "authToken")}))))))

(defmethod read :board/list [{:keys [ast target query state db-path] :as env} k _]
  (let [st @state]
    (cond-> {}
      (nil? target)
      (assoc :value (denorm-data-val k env))
      (not= nil target)
      (assoc target (assoc ast :query-root true)))))

(defmethod read :column/list [{:keys [ast target route query state db-path] :as env} k params]
  (let [st @state]
    (cond-> {}
      (not= nil target)
      (assoc target (-> ast
                        (assoc :query-root true)
                        (assoc :params (second route))))
      (nil? target)
      (assoc :value (denorm-data-val k env)))))

(defn get-query-root
  [{:keys [ast target parser] :as env}]
  {target (update-in ast [:query]
                     #(let [q (if (vector? %) % [%])
                            res (parser env q target)]
                        res))})

(defn read-local-value [{:keys [target state query parser db-path] :as env}]
  (println db-path)
  (let [st @state
        parsed (parser env query)
        current (get-in st db-path)]
    (cond-> current
      (map? current) (merge parsed))))

(defmethod read :app/local-state
  [env k _]
  {:value (read-local-value (assoc env :db-path [k]))})

(defmethod read :default
  [{:keys [target state query parser db-path] :as env} k _]
  (let [st @state
        db-path' (conj (or db-path []) k)
        env' (assoc env :db-path db-path' )]
    (cond-> {}
      (and (not= nil target) (not= nil query))
      (merge (get-query-root env'))
      (nil? target)
      (assoc :value (read-local-value env')))))

(defmethod read :route/data 
  [{:keys [target parser state query ast] :as env} k params]
  (let [st @state
        route (-> (get st :app/route) first)
        route-params (-> (get st :app/route) second)
        env' (-> env
                 (assoc :route [route route-params])
                 (assoc :db-path [:route/data]))
        query' (into {} (filter #(not-empty (get % route)) query))]

    (if (nil? route)
      {:value nil}
      (let [parsed (parser env' [query'] target)
            sub-query (-> (assoc ast :query parsed)
                          (parser/ast->expr)
                          (parser/expr->ast))]
        (cond-> {}
          (not= nil target) (assoc target sub-query)
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
  [{:keys [state]} _ {:keys [field field-state ident] :as incm}]
  (let [state' @state
        route (first (:app/route state'))]
    {:keys [:route/data]
     :action (fn []
               (swap! state assoc-in [:app/local-state field] {:state field-state})
               (swap! state assoc-in [:app/local-state :field-idents] {field ident}))}))

; todo: include db-path (current route) in env
(defmethod mutate 'local/update-order!
  [{:keys [state]} _ {:keys [target-column-id target-task-id extra]}]
  (let [st @state
        route (-> (get st :app/route) first)
        column-dragging? (= :drag-start (get-in st [:app/local-state :column/moving :state]))
        task-dragging? (= :drag-start (get-in st [:app/local-state :task/moving :state]))
        dragged-column-id (get-in st [:app/local-state :field-idents :column/moving :column-id])
        dragged-task-id (get-in st [:app/local-state :field-idents :task/moving :task-id])]
    {:action
     (fn []
       (cond
         (and column-dragging? target-column-id (not= dragged-column-id target-column-id))
         (uo/update-order! state {:dragged-column-id dragged-column-id
                                  :target-column-id target-column-id})
         
         (and task-dragging? target-column-id)
         (uo/update-order! state {:dragged-task-id dragged-task-id
                                  :target-column-id target-column-id})
         
         (and task-dragging? target-task-id)
         (uo/update-order! state {:dragged-task-id dragged-task-id
                                  :target-task-id target-task-id
                                  :direction (get-in extra [:direction])})))}))

(defmethod mutate 'local/store-user!
  [{:keys [state]} _ {:keys [user/email user/userid user/token] :as d}]
  {:action (fn []
             (swap! state assoc :oauth/user d))})

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
