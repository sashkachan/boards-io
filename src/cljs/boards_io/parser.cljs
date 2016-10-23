(ns boards-io.parser
  (:require 
   [om.next :as om]
   [goog.log :as glog]
   [om.next.impl.parser :as parser]))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :board/list [{:keys [ast target] :as env} _ _]
  {target (assoc ast :query-root true)})

(defmethod read :column/list [{:keys [ast target route] :as env} _ params]
  (println (second route))
  {target (-> ast
              (assoc :query-root true)
              (assoc :params (second route)))})

(defn get-query-root
  [{:keys [ast target parser] :as env}]
  {target (update-in ast [:query]
                     #(let [q (if (vector? %) % [%])
                            res (parser env q target)
                            _ (println "query: " q)
                            _ (println "res:   " res)]
                        res))})


(defmethod read :boards
  [{:keys [target state query parser] :as env} k _]
  (if (not= nil target)
    (get-query-root env)
    {:value (merge (get @state :boards) (parser env query))}))

(defmethod read :default
  [{:keys [target state query parser] :as env} k _]
  (if (not= nil target)
    (get-query-root env)
    {:value (get @state k)}))

(defmethod read :route/data 
  [{:keys [target parser state query ast] :as env} k params]
  (let [st @state
        route (-> (get st :app/route) first)
        route-params (-> (get st :app/route) second)
        env' (assoc env :route [route route-params])
        query' (get query route)]
    (if (nil? route)
      {:value nil}
      (let [parsed (parser env' [{route query'}] target)]
        (cond-> {}
          (not= nil target) (assoc target (parser/expr->ast (first parsed)))
          (= nil target ) (assoc :value parsed))))))

(defmethod read :app/local-state
  [{:keys [state env query]} key _]
  (let [st @state]
    (println "reading :app/local-state")
    {:value (get st :app/local-state)}))

(defmethod read :app/route
  [{:keys [state query]} k _]
  (let [st @state]
     {:value (get st k)}))

(defmethod mutate 'change/route!
  [{:keys [state]} _ {:keys [route]}]
  {:keys [:boards :columns]
   :action (fn []
             (swap! state assoc :app/route route))})

(defmethod mutate 'change/toggle-modal!
  [{:keys [state]} _ {:keys [modal modal-state]}]
  {:keys [:app/local-state]
   :action (fn []
             (let [loc-state (get @state :app/local-state)
                   new-state (if loc-state loc-state {})
                   new-modal-state (assoc new-state modal modal-state)]
               (swap! state assoc :app/local-state new-modal-state))
             )})
