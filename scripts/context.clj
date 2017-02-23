(remove-ns 'dev)

(ns dev
  (:require [boards-io.system :as s]
            [boards-io.handler :as h]
            [boards-io.parser :as parser]
            [datomic.api :as d]
            [om.next.server :as om]))

(in-ns 'dev)
(def token "token-yada-yada")

(def conn (-> s/system :db :connection))

(def parser (om/parser {:read parser/readf :mutate parser/mutatef}))


(parser/save-token {:user_id "123" :email "hello@example.com" } token (-> s/system :db :connection))

(parser
 {:auth-token token
  :conn (-> s/system :db :connection)}
 '[{:board/list [:db/id :board/name :board/description]}])

(parser
 {:auth-token token
  :conn (-> s/system :db :connection)}
 '[(save/new-board! {:title "Title" :description "Description"})])

(parser
 {:auth-token token
  :conn (-> s/system :db :connection)}
 '[(save/new-column! {:title "Title" :order 2 :board-id 17592186045427})])

