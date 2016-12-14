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

(defmethod read :column/list [{:keys [ast target route] :as env} _ params]
  {target (-> ast
              (assoc :query-root true)
              (assoc :params (second route)))})

(defn get-query-root
  [{:keys [ast target parser] :as env}]
  #_(println "get-query-root ast " ast)
  {target (update-in ast [:query]
                     #(let [q (if (vector? %) % [%])
                            res (parser env q target)]
                        #_(println "in update get-query-root " res)
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

(defmethod mutate :default
  [{:keys [state ref] :as env} _ _]
  {:remote true})
