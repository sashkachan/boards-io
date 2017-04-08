(ns boards-io.modals
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui ui]]
            [cljsjs.react-bootstrap]
            [boards-io.handlers :as h]))

(def bs-modal (js/React.createFactory js/ReactBootstrap.Modal))
(def bs-button (js/React.createFactory js/ReactBootstrap.Button))
(def bs-button-toolbar (js/React.createFactory js/ReactBootstrap.ButtonToolbar))
(def bs-modal-title (js/React.createFactory js/ReactBootstrap.Modal.Title))
(def bs-modal-header (js/React.createFactory js/ReactBootstrap.Modal.Header))
(def bs-modal-body (js/React.createFactory js/ReactBootstrap.Modal.Body))
(def bs-modal-footer (js/React.createFactory js/ReactBootstrap.Modal.Footer))
(def bs-overlay (js/React.createFactory js/ReactBootstrap.Overlay))
(def bs-popover (js/React.createFactory js/ReactBootstrap.Popover))
(def bs-overlay-trigger (js/React.createFactory js/ReactBootstrap.OverlayTrigger))
(def bs-form-group (om/factory js/ReactBootstrap.FormGroup))

(defn save [b submit-fn env]
  (bs-button #js {:bsStyle "primary"
                  :onClick #(submit-fn env)
                  :key "mod-btn2"} b))

(defui NewColumnForm
  Object
  (render [this]
          (let [{:keys [extras root]} (om/props this)]
            (dom/form
             #js {:id "new-column-form"}
             (dom/div #js {:className "form-group" :key "new-column-form-div"}
                      [(bs-form-group #js {:key "new-col-grp"}
                                      (dom/input #js {:type "text"
                                                      :name "column-title"
                                                      :className "form-control"
                                                      :placeholder "Title"
                                                      :key "new-column-form-div-inp1"}
                                                 ))
                       (bs-button
                        #js {:bsStyle "primary"
                             :onClick #(h/new-column-save {:reconciler root
                                                           :ref :column/new-column-modal
                                                           :extras extras}
                                                          )
                             :key "mod-btn2"} "Save")]))
            )))


(def new-column-form (om/factory NewColumnForm))

(defui NewBoardForm
  Object
  (render [this]
          (let [{:keys [root]} (om/props this)]
            (dom/form
             #js {:id "new-board-form"}
             (dom/div #js {:className "form-group"}
                      [(bs-form-group #js {:key "new-board-form-grp"}
                                      (dom/input #js {:type "text"
                                                      :name "board-title"
                                                      :className "form-control"
                                                      :placeholder "Title"
                                                      :key "new-board-form-div-inp1"}
                                                 ))

                       (bs-button #js {:bsStyle "primary"
                                       :onClick #(h/new-board-save {:reconciler root
                                                                    :ref :board/new-board-modal}
                                                                   )
                                       :key "mod-btn2"} "Save")
                       ])))))

(def new-board-form (om/factory NewBoardForm))

(defui NewTaskForm
  Object
  (render [this]
          (let [{:keys [extras root]} (om/props this)]
            (dom/form
             #js {:id "new-task-form"}
             (dom/div #js {:className "form-group"}
                      [(bs-form-group #js {:key "new-task-grp"}
                                      (dom/input #js {:type "text"
                                                      :name "task-title"
                                                      :className "form-control"
                                                      :placeholder "Title"
                                                      :key "new-task-form-div-inp1"}
                                                 ))
                       (bs-button
                        #js {:bsStyle "primary"
                             :onClick #(h/new-task-save {:component root
                                                         :ref :column/new-task-modal
                                                         :extras extras})
                             :key "mod-btn2"} "Save")]))
            )))

(def new-task-form (om/factory NewTaskForm))

(defui OverlayHandler
  Object
  (componentWillReceiveProps
   [this props]
   (let [curr-state (om/get-state this)]
     (when-not (= (:show props) (:show curr-state)) 
       (om/update-state! this #(assoc % :show (:show props))))))
  
  (initLocalState [this]
                  {:show false :target nil})
  (render [this]
          (let [{:keys [title id placement trigger hide-fn show-fn]} (om/props this)
                on-hide #(do (om/set-state! this {:show false :target nil})
                             (hide-fn))]
            (dom/div #js {}
                     [(bs-button
                       #js {:bsStyle "link"
                            :onClick #(when-not (= true (om/get-state this :show))
                                        (om/set-state! this {:show true :target (.-target %)})
                                        (show-fn))}
                       title)
                      (bs-overlay #js {:show (om/get-state this :show)
                                       :placement placement
                                       :target (om/get-state this :target)
                                       :onHide on-hide
                                       :animation false
                                       :rootClose true }
                                  (bs-popover #js {:id id }
                                              (om/children this))
                                  )]))))

(def overlay-handler (om/factory OverlayHandler))

(defui Modal
  Object
  (render
   [this]
   
   (let [{:keys [save-btn-state ref modal-content submit-fn title extras show] :as props} (om/props this)

         h-env {:reconciler (om/get-reconciler this)
                :save-btn-field :board/save-btn-field
                :ref ref
                :extras extras}
         close (fn [b] (bs-button #js {:key "mdl-close" :onClick #(h/modal-close h-env)} b))
         ]
     (bs-modal #js {:show show :onHide #(h/modal-close h-env)}
               [(bs-modal-header #js {:closeButton true :key "modal-header"}
                                 (bs-modal-title nil (dom/h4 #js {:key "new-board-save-h41"} title)))
                (bs-modal-body #js {:key "modal-body"} modal-content)
                (bs-modal-footer #js {:key "modal-footer"}
                                 [(close "Close")
                                  #_(save "Save")])]))))
