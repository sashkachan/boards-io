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
  (let [resp (d/q '[:find [(pull ?eid sl) ...] :in $ sl :where [ ?eid :board/name] ] (d/db conn) query)]
    {:value resp})
  )

(defmethod readf :column/list
  [{:keys [conn query]} k params]
  (let [{:keys [board-id]} params]
    (println ":column/list " query params board-id)
    {:value (d/q '[:find [(pull ?cid sl) ...] :in $ sl ?bid :where [ ?cid :column/name] [ ?cid :column/board ?bid]] (d/db conn) query (read-string board-id))})
  )



;; =============================================================================
;; Mutations

(defmulti mutatef (fn [env k params] k))

(defmethod mutatef :default
  [_ k _]
  {:value {:error (str "No handler for mutation key " k)}})
