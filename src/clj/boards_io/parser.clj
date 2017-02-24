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
  [{:keys [auth-token conn query auth-token]} k _]
  (println "baord/list uid " auth-token)
  (let [resp (d/q '[:find [(pull ?eid q) ...]
                    :in $ q ?token
                    :where
                    [ ?eid :board/name]
                    [ ?eid :board/user ?uid]
                    [ ?uid :user/token ?token]] (d/db conn) query auth-token)]
    {:value resp}))

(defmethod readf :column/list
  [{:keys [conn query auth-token]} k params]
  (let [{:keys [board-id]} params]
    {:value (d/q '[:find [(pull ?cid q) ...]
                   :in $ ?bid q ?token
                   :where
                   [ ?cid :column/name]
                   [ ?cid :column/board ?bid]
                   [ ?bid :board/user ?uid]
                   [ ?uid :user/token ?token]
                   ;[ ?tid :task/name ]
                   ]
                 (d/db conn) (read-string board-id) query auth-token)}))

(defmethod readf :oauth/user
  [{:keys [conn query]} k params]
  (let [{:keys [token]} params]
    {:value (if token
              (first
               (d/q '[:find [(pull ?uid q) ...]
                      :in $ ?token q
                      :where
                      [?uid :user/token ?token]]
                    (d/db conn) token query)))}))



;; =============================================================================
;; Mutations

(defmulti mutatef (fn [env k params] k))

(defmethod mutatef 'save/new-board!
  [{:keys [conn auth-token] :as env} k {:keys [title description] :as params}]
  {:value {:keys '[:board/list]}
   :action (fn []
              @(d/transact conn `[{:db/id #db/id[:db.part/user]
                                   :board/name ~title
                                   :board/description ~description
                                   :board/user [:user/token ~auth-token]
                                   }]))
   })

(defmethod mutatef 'save/new-task!
  [{:keys [conn] :as env} k {:keys [title column-id order] :as params}]
  (println " -- save/new-task! " env params)
  {:value {:keys '[:column/list]}
   :action (fn []
             @(d/transact conn `[{:db/id #db/id[:db.part/user -10001]
                                  :task/name ~title
                                  :task/order ~order}
                                 [:db/add ~column-id :column/tasks #db/id[:db.part/user -10001]]]))}
  
  )

(defmethod mutatef 'save/new-column!
  [{:keys [conn auth-token] :as env} k {:keys [title board-id order] :as params}]
  (println " -- save/new-column! " env params)
  {:value {:keys '[:column/list]}
   :action (fn []
             @(d/transact conn `[{:db/id #db/id[:db.part/user]
                                  :column/name ~title
                                  :column/board ~board-id
                                  :board/user [:user/token ~auth-token]
                                  :column/order ~order}]))})

(defmethod mutatef 'save/update-order-tasks!
  [{:keys [conn] :as env} k {:keys [tasks] :as params}]
  (println "save/update-order-tasks! " tasks)
    {:value {:keys '[:task/list]}
     :action (fn []
               (if (not= nil tasks)
                 @(d/transact conn tasks)))})

(defmethod mutatef 'save/remove-column-task!
  [{:keys [conn] :as env} k {:keys [cid tid] :as params}]
  {:action (fn []
             @(d/transact conn [:db/retract (read-string cid) :column/tasks (read-string tid)]))})


(defmethod mutatef 'save/add-column-task!
  [{:keys [conn] :as env} k {:keys [cid tid] :as params}]
  {:action (fn []
             @(d/transact conn [:db/add (read-string cid) :column/tasks (read-string tid)]))})

(defmethod mutatef 'save/update-order-columns!
  [{:keys [conn] :as env} k {:keys [columns] :as params}]
  (println "save/update-order-columns! " columns)
    {:value {:keys '[:column/list]}
     :action (fn []
               (if (not= nil columns)
                 @(d/transact conn columns)))})


(defn save-token [{:keys [user_id email] :as token-data} token conn]
  (println token-data token )
  (d/transact conn `[{:db/id #db/id[:db.part/user]
                      :user/email ~email
                      :user/userid ~user_id
                      :user/token ~token}]))
