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
#_    (println k "read-route parsed: " parsed)
#_    (println "   read-route ast: " ast)
      (if (and (not (empty? parsed)) (not= nil target))
        (let [fps (first parsed)
              expr-ast (parser/expr->ast (first parsed))]
          {target expr-ast})
        {:value parsed})))

(defmethod read :boards
  [env k params]
  (read-route env k params))

(defmethod read :columns
  [env k params]
  (read-route env k params))


(defmethod read :default
  [{:keys [state route target query ast]} k params]
  (let [st @state]
    (if (nil? (get st k))
      {target (assoc ast :query-root true)}
      {:value (get st k)})))

(defmethod read :route/data 
      [{:keys [target parser state query ast] :as env} k _]
  (let [st @state
        route (-> (get st :app/route) first)
        env' (assoc env :route route)
        query' (or (get query route) query) ;; why is query already parsed?
        ]
#_    (println " route: " route)
#_    (println " query: " query " remote " target)
#_    (println "query': " query' " remote " target)
#_    (println " state: " st)
    (let [query-or-val (parser env' [{route query'}] target)]
#_      (println ":route/data parser: " query-or-val)
      (if (not= nil target)
        (cond-> {target nil}
          (not (empty? query-or-val)) (assoc target (parser/expr->ast (first query-or-val))))
        {:value (get query-or-val route)}))))

(defmethod read :app/route
  [{:keys [state query]} k _]
  (let [st @state]
     {:value (get st k)}))

(defmethod mutate 'change/route!
  [{:keys [state]} _ {:keys [route]}]
  {:keys [:route/data :columns :boards]
   :action (fn []
             (swap! state assoc :app/route route))})
