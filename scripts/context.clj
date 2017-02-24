(remove-ns 'dev)

(ns dev
  (:require [boards-io.system :as s]
            [boards-io.handler :as h]
            [boards-io.parser :as parser]
            [clojure.pprint :as pp]
            [datomic.api :as d]
            [om.next.server :as om]))

(in-ns 'dev)
(def token "token-yada-yada")

(def conn (-> s/system :db :connection))

(def parser (om/parser {:read parser/readf :mutate parser/mutatef}))


#_(parser/save-token {:user_id "123" :email "hello@example.com" } token (-> s/system :db :connection))

#_(parser
 {:auth-token token
  :conn (-> s/system :db :connection)}
 '[{:board/list [:db/id :board/name :board/description]}])

#_(parser
 {:auth-token token
  :conn (-> s/system :db :connection)}
 '[(save/new-board! {:title "Title" :description "Description"})])

#_(parser
 {:auth-token token
  :conn (-> s/system :db :connection)}
 '[(save/new-column! {:title "Title" :order 2 :board-id 17592186045427})])


#_(pp/pprint (d/q '[:find [(pull ?cid [:column/name :column/board {:column/tasks [:task/name :task/order :db/id]}])] :where [?cid :column/name]] (d/db conn)))
