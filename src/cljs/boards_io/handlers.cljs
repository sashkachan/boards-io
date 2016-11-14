(ns boards-io.handlers
  (:require [om.next :as om]
            [goog.dom :as gdom]
            [goog.dom.forms :as forms]))

(defn change-route! [{:keys [reconciler matcher query-root this]} route]
  (let [{:keys [handler route-params]} (matcher route)
        ;;root (:root @(:state reconciler))
        ]
    (om/transact! this `[(local/route! {:route [~handler ~route-params]})
                         ~query-root
                         ])))

(defn modal-open [{:keys [reconciler ref ident]}]
  (om/transact! reconciler
   `[(local/toggle-field! {:field ~ref :field-state 1 :ident ~ident})]))

(defn modal-close [{:keys [reconciler ref]} ]
  (om/transact! reconciler `[(local/toggle-field! {:field ~ref :field-state 0})]))

(defn new-board-save [{:keys [reconciler root-query save-btn-field idents] :as env}]
  (println "Root-query " root-query save-btn-field)
  (let [form (gdom/getElement "new-board-form")
        title (forms/getValueByName form "board-title")
        description (forms/getValueByName form "board-description")]
    (om/transact! reconciler
                  `[(local/toggle-field! {:field ~save-btn-field :field-state :off})
                    (save/new-board! {:title ~title :description ~description})
                    ~root-query
                    (local/toggle-field! {:field ~save-btn-field :field-state :on})
                    ])
    (modal-close (assoc env :ref :board/new-board-modal))))

(defn new-task-save [{:keys [reconciler root-query save-btn-field extras] :as env}]
  ; todo: if nil? extras -> exception!
  (let [column-id (:column-id extras)
        form (gdom/getElement "new-task-form")
        title (forms/getValueByName form "task-title")]
    (om/transact! reconciler
                  `[(local/toggle-field! {:field ~save-btn-field :field-state :off})
                    (save/new-task! {:title ~title :column-id ~column-id})
                    ~root-query
                    (local/toggle-field! {:field ~save-btn-field :field-state :on})
                    ])
    (modal-close env)))
