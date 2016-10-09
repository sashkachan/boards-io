(ns boards-io.parser
  (:require 
   [om.next :as om]
   [om.next.impl.parser :as parser]))

(defn- infer-query
  [{:keys [query]} route]
  (when-let [subq (cond-> query
                    (map? query) (get route))]
    subq))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default
  [{:keys [state]} k params]
  ;;(println "Read with " k)
  (let [st @state]
    {:value (get st k)
     :remote true}))

(defmethod read :route/data
  [{:keys [target state query parser] :as env} k _]
  ;;(println "reading route/data " k " st " @state " target " target)
  (let [st @state
        route (-> st :app/route first)]
    (if (= :remote target)
      (let [inf-query (infer-query env route)
            _ (println "Infer query " query)
            ret ((:parser env) env inf-query :remote)]
        {:remote (parser/expr->ast (first ret))})
      {:value {route (get st route)}})))

(defmethod read :app/route
  [{:keys [state query]} k _]
  (let [st @state]
     {:value (get st k)}))

(defmethod mutate 'change/route!
  [{:keys [state]} _ {:keys [route]}]
  (println "mutate change/route! " route)
  {:value {:keys [:app/route]}
   :action #(swap! state assoc :app/route route)})
