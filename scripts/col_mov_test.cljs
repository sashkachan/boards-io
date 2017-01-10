(in-ns 'boards-io.core
       (:require [om.next :as om]))

(om/transact! reconciler '[(local/toggle-field! {:field :column/moving :field-state :drag-start :ident {:column-id 17592186045421}})])

(om/transact! reconciler '[(local/toggle-field! {:field :column/moving :field-state :drag-end :ident {:column-id 17592186045420}})])

(om/transact! reconciler '[(local/update-order! {:target-column-id 17592186045420})])


(get-in @reconciler [:route/data :columns :column/by-id 17592186045420 :column/order])
(get-in @reconciler [:route/data :columns :column/by-id 17592186045422 :column/order])
(get-in @reconciler [:route/data :columns :column/by-id 17592186045423 :column/order])

(into {} (map (fn [[column-id col-map]] {column-id (:column/order col-map)})  (get-in @reconciler [:route/data :columns :column/by-id])))
