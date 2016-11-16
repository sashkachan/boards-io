(ns
    boards-io.parser
  (:refer-clojure :exclude [read])
  (:require [datomic.api :as d]))

;; =============================================================================
;; Reads

(defmulti readf (fn [env k params] k))

(defmethod readf :default
  [{:keys [conn query]} k _]
  {:value {:error (str "No handler for read key " k)}})

(defmethod readf :board/list
  [{:keys [conn query]} k _]
  (let [resp (d/q '[:find [(pull ?eid q) ...]
                    :in $ q
                    :where [ ?eid :board/name] ] (d/db conn) query)]   
    {:value resp}))

(defmethod readf :column/list
  [{:keys [conn query]} k params]
  (let [{:keys [board-id]} params]
    (println ":column/list " query  params board-id)
    {:value (d/q '[:find [(pull ?cid q) ...]
                   :in $ ?bid q
                   :where
                   [ ?cid :column/name]
                   [ ?cid :column/board ?bid]
                   [ ?tid :task/name ]]
                 (d/db conn) (read-string board-id) query)}))



;; =============================================================================
;; Mutations

(defmulti mutatef (fn [env k params] k))

(defmethod mutatef 'save/new-board!
  [{:keys [conn] :as env} k {:keys [title description] :as params}]
  (println " -- save/new-board! " env params)
  
  {:value {:keys '[:board/list]}
   :action (fn []
              @(d/transact conn `[{:db/id #db/id[:db.part/user]
                                   :board/name ~title
                                   :board/description ~description}]))
   })

(defmethod mutatef 'save/new-task!
  [{:keys [conn] :as env} k {:keys [title column-id] :as params}]
  (println " -- save/new-task! " env params)
  {:value {:keys '[:column/list]}
   :action (fn []
             @(d/transact conn `[{:db/id #db/id[:db.part/user]
                                  :task/name ~title
                                  :task/column ~column-id}]))}
  
  )

(defmethod mutatef 'save/new-column!
  [{:keys [conn] :as env} k {:keys [title board-id] :as params}]
  (println " -- save/new-column! " env params)
  {:value {:keys '[:column/list]}
   :action (fn []
             @(d/transact conn `[{:db/id #db/id[:db.part/user]
                                  :column/name ~title
                                  :column/board ~board-id}]))})
