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
  (let [resp (d/q '[:find [(pull ?eid q) ...]
                    :in $ q ?token
                    :where
                    [ ?eid :board/name]
                    [ ?eid :board/user ?uid]
                    [ ?uid :user/token ?token]] (d/db conn) query (or auth-token ""))]
    {:value resp}))

(defmethod readf :board/by-id
  [{:keys [auth-token conn query]} k {:keys [board/by-id]}]
  (let [resp (d/q '[:find [(pull ?eid q) ...]
                    :in $ q ?token ?eid
                    :where
                    [ ?eid :board/name]
                    [ ?eid :board/user ?uid]
                    [ ?uid :user/token ?token]] (d/db conn) query (or auth-token "") (read-string by-id))]
    {:value resp}))

(defmethod readf :column/list
  [{:keys [conn query auth-token]} k params]
  (let [{:keys [board/by-id]} params]
    {:value (d/q '[:find [(pull ?cid q) ...]
                   :in $ ?bid q ?token
                   :where
                   [ ?cid :column/name]
                   [ ?cid :column/board ?bid]
                   [ ?bid :board/user ?uid]
                   [ ?uid :user/token ?token]
                   ]
                 (d/db conn) (read-string by-id) query auth-token)}))

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
  [{:keys [conn auth-token] :as env} k {:keys [title] :as params}]
  {:value {:keys '[:board/list]}
   :action (fn []
             (when auth-token
               (let [resp @(d/transact conn `[{:db/id #db/id[:db.part/user]
                                               :board/name ~title
                                               :board/user [:user/token ~auth-token]
                                               }]
                                       )]))
             {})
   })

(defmethod mutatef 'save/new-task!
  [{:keys [conn] :as env} k {:keys [title column-id order] :as params}]
  {:value {:keys '[:column/list]}
   :action (fn []
             (d/transact conn `[{:db/id #db/id[:db.part/user -10001]
                                 :task/name ~title
                                 :task/order ~order}
                                [:db/add ~column-id :column/tasks #db/id[:db.part/user -10001]]])
             {})}
  
  )

(defmethod mutatef 'save/new-column!
  [{:keys [conn auth-token] :as env} k {:keys [title board-id order] :as params}]
  {:value {:keys '[:column/list]}
   :action (fn []
             (d/transact conn `[{:db/id #db/id[:db.part/user]
                                 :column/name ~title
                                 :column/board ~board-id
                                 :board/user [:user/token ~auth-token]
                                 :column/order ~order}])
             {})})

(defmethod mutatef 'save/update-order-tasks!
  [{:keys [conn] :as env} k {:keys [tasks] :as params}]
    {:value {:keys '[:task/list]}
     :action (fn []
               (if (not= nil tasks)
                 (d/transact conn tasks))
               {})})

(defmethod mutatef 'save/remove-column-task!
  [{:keys [conn] :as env} k {:keys [cid tid] :as params}]
  {:action (fn []
             (d/transact conn [[:db/retract cid :column/tasks tid]])
             {})})


(defmethod mutatef 'save/add-column-task!
  [{:keys [conn] :as env} k {:keys [cid tid] :as params}]
  {:action (fn []
             (d/transact conn [[:db/add cid :column/tasks tid]])
             {})})

(defmethod mutatef 'save/update-order-columns!
  [{:keys [conn] :as env} k {:keys [columns] :as params}]
    {:value {:keys '[:column/list]}
     :action (fn []
               (if (not= nil columns)
                 (d/transact conn columns))
               {})})


(defn save-token [{:keys [user_id email] :as token-data} token conn]
  (d/transact conn `[{:db/id #db/id[:db.part/user]
                      :user/email ~email
                      :user/userid ~user_id
                      :user/token ~token}]))
