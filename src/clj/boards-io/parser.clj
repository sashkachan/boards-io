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
  (println query)
  (let [resp (d/q '[:find [(pull ?eid sl) ...] :in $ sl :where [ ?eid :board/name] ] (d/db conn) query)]
    {:value resp})
  )

(defmethod readf :column/list
  [{:keys [conn query]} k params]
  (println "params: " params )
  (println "query:" query)
  {:value (d/q '[:find [(pull ?cid sl) ...] :in $ sl ?board :where [ ?cid :column/name] [ ?cid :column/board ?board]] (d/db conn) (:column/list (first query)) (read-string (or (:board-id (first query)) "0")))}
  )



;; =============================================================================
;; Mutations

(defmulti mutatef (fn [env k params] k))

(defmethod mutatef :default
  [_ k _]
  {:value {:error (str "No handler for mutation key " k)}})
