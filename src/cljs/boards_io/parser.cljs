(ns boards-io.parser
  (:require 
   [om.next :as om]
   [goog.log :as glog]
   [om.next.impl.parser :as parser]))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defn read-route [{:keys [state target query parser ast] :as env} k params]
  (let [st @state
        parsed (parser env query target)]
#_      (println "   read-route ast: " ast)
      (if (and (not (empty? parsed)) (not= nil target))
        (let [fps (first parsed)
              ;; add cond-> and inject params
              expr-ast (cond-> (parser/expr->ast (first parsed))
                         (not= nil params) (assoc :params params))]
          {target expr-ast})
        {:value parsed})))

(defmethod read :boards
  [env k params]
  (read-route env k params))

(defmethod read :columns
  [env k params]
  (let [rp (-> env :route second)]
    (read-route env k (merge params rp))))


(defmethod read :default
  [{:keys [state route target query ast]} k params]
  (let [st @state]
    (if (nil? (get st k))
      {target (assoc ast :query-root true)}
      {:value (get st k)})))

(defmethod read :route/data 
      [{:keys [target parser state query ast] :as env} k params]
  (let [st @state
        route (-> (get st :app/route) first)
        route-params (-> (get st :app/route) second)
        env' (assoc env :route [route route-params]) 
        query' (get query route)]
    (println "route/data params " route-params)
;;    (println "   ast: " ast)
;;    (println " route: " route)
;;    (println " query: " query " remote " target)
;;    (println "query': " query' " remote " target)
;;    (println " state: " st)
    (if (not= nil target)
      (let [parsed (parser env' [{route query'}] target)]
        (cond-> {target nil}
          (not (empty? parsed)) (assoc target (parser/expr->ast (first parsed)))))
      {:value (get st :route/data)})))

(defmethod read :app/route
  [{:keys [state query]} k _]
  (let [st @state]
     {:value (get st k)}))

(defmethod mutate 'change/route!
  [{:keys [state]} _ {:keys [route]}]
  {:keys [:route/data]
   :action (fn []
             (swap! state assoc :app/route route))})
