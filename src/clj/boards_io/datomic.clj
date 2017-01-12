(ns boards-io.datomic)

(def schema-tx
  '[
    ;; board
    {:db/id #db/id[:db.part/db]
     :db/ident :board/name
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/fulltext true
     :db/doc "A board name"
     :db.install/_attribute :db.part/db}
    
    {:db/id #db/id[:db.part/db]
     :db/ident :board/description
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/fulltext true
     :db/doc "A board description"
     :db.install/_attribute :db.part/db}

    ;; column
    {:db/id #db/id[:db.part/db]
     :db/ident :column/name
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/fulltext true
     :db/doc "A column name"
     :db.install/_attribute :db.part/db}
    
    {:db/id #db/id[:db.part/db]
     :db/ident :column/board
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/fulltext true
     :db/doc "A column board"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :column/order
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/fulltext true
     :db/doc "A column order"
     :db.install/_attribute :db.part/db}
    
    ;; task
    {:db/id #db/id[:db.part/db]
     :db/ident :task/name
     :db/valueType :db.type/string
     :db/cardinality :db.cardinality/one
     :db/fulltext true
     :db/doc "A task name"
     :db.install/_attribute :db.part/db}

    {:db/id #db/id[:db.part/db]
     :db/ident :task/order
     :db/valueType :db.type/long
     :db/cardinality :db.cardinality/one
     :db/fulltext true
     :db/doc "A task order"
     :db.install/_attribute :db.part/db}
    
    {:db/id #db/id[:db.part/db]
     :db/ident :task/column
     :db/valueType :db.type/ref
     :db/cardinality :db.cardinality/one
     :db/fulltext true
     :db/doc "A task column"
     :db.install/_attribute :db.part/db}])



(def initial-data '[{:db/id #db/id[:db.part/user]
                     :board/name "Work"
                     :board/description "Work board"}
                    
                    {:db/id #db/id[:db.part/user -100001]
                     :board/name "Personal"
                     :board/description "Personal board"}

                    {:db/id #db/id[:db.part/user -20001]
                     :column/board #db/id[:db.part/user -100001]
                     :column/name "To-Doskis"
                     :column/order 2}

                    {:db/id #db/id[:db.part/user -20002]
                     :column/board #db/id[:db.part/user -100001]
                     :column/name "Doing"
                     :column/order 3}
                    
                    {:db/id #db/id[:db.part/user]
                     :column/board #db/id[:db.part/user -100001]
                     :column/name "Backlog"
                     :column/order 4}
                    
                    {:db/id #db/id[:db.part/user]
                     :column/board #db/id[:db.part/user -100001]
                     :column/name "Archived"
                     :column/order 5}

                    {:db/id #db/id[:db.part/user]
                     :task/name "Do the laundry"
                     :task/order 1
                     :task/column #db/id[:db.part/user -20001]
                     }

                    {:db/id #db/id[:db.part/user]
                     :task/name "Take a shower"
                     :task/order 2
                     :task/column #db/id[:db.part/user -20001]
                     }

                    {:db/id #db/id[:db.part/user]
                     :task/name "Do some work"
                     :task/order 1
                     :task/column #db/id[:db.part/user -20002]
                     }
                    
                    ])
