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
  (let [resp (d/q '[:find [(pull ?eid [*]) ...] :in $ :where [ ?eid :board/name] ] (d/db conn))]
    
    {:value resp}))

(defmethod readf :column/list
  [{:keys [conn]} k params]
  (let [{:keys [board-id]} params]
    (println ":column/list "  params board-id)
    {:value (d/q '[:find [(pull ?cid [:db/id :column/name {:column/board [*]} {:task/_column [*]}]) ...]
                   :in $ ?bid
                   :where
                   [ ?cid :column/name]
                   [ ?cid :column/board ?bid]
                   [ ?tid :task/name ]]
                 (d/db conn) (read-string board-id))}))



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
