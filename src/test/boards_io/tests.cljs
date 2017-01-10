(ns boards-io.tests
  (:require 
            [clojure.test :refer [deftest is are testing run-tests]]
            [boards-io.parser :as p]
            [boards-io.components :as c]
            [boards-io.handlers :as h]
            [om.next :as om :refer-macros [defui ui]]
            ))

(def test-state
  {:app/route [:columns {:board-id "17592186045419"}],
   :route/data
   {:columns
    {:column/list
     [[:column/by-id 17592186045420]
      [:column/by-id 17592186045421]
      [:column/by-id 17592186045422]
      [:column/by-id 17592186045423]],
     :column/by-id
     {17592186045420
      {:db/id 17592186045420,
       :column/name "To-Doskis",
       :column/order 2,
       :column/board [:board/by-id 17592186045419],
       :task/_column
       [{:task/name "Do the laundry"} {:task/name "Take a shower"}]},
      17592186045421
      {:db/id 17592186045421,
       :column/name "Doing",
       :column/order 3,
       :column/board [:board/by-id 17592186045419],
       :task/_column [{:task/name "Do some work"}]},
      17592186045422
      {:db/id 17592186045422,
       :column/name "Backlog",
       :column/order 4,
       :column/board [:board/by-id 17592186045419]},
      17592186045423
      {:db/id 17592186045423,
       :column/name "Archived",
       :column/order 5,
       :column/board [:board/by-id 17592186045419]}},
     :om.next/tables #{:board/by-id :column/by-id},
     :board/by-id
     {17592186045419
      {:db/id 17592186045419,
       :board/name "Personal", 
       :board/description "Personal board"}}}}})

(defn get-reconciler [state]
  (om/reconciler
   {:state state
    :parser (om/parser {:read p/read :mutate p/mutate})
    :normalize true
    :id-key :db/id
    :merge  (p/merger c/route->component)
    ;:send (transit/transit-post "/api")
    }))

(deftest test-reordering
  (let [state (atom test-state)
        reconc (get-reconciler state)
        _ (om/transact! reconc '[(local/toggle-field! {:field :column/moving :field-state :drag-start :ident {:column-id 17592186045420}})])
        _ (h/update-order {:reconciler reconc :target-column-id 17592186045421})
        get-idents #(get-in @reconc [:route/data :columns :app/local-state :field-idents])
        ]
    (testing "moving column is set to state"
      (is (= (get-idents)
             {:column/moving {:column-id 17592186045420}})))
    (testing "idents returns map" (is (= true (map? (get-idents)))))
    (testing "order swapped correctly"
      (is (= 3 (get-in @reconc [:route/data :columns :column/by-id 17592186045420 :column/order])))
      (is (= 2 (get-in @reconc [:route/data :columns :column/by-id 17592186045421 :column/order]))))))
