(ns boards-io.modals
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui ui]]
            [cljsjs.react-bootstrap]
            [boards-io.handlers :as h]))


(def new-board-form
  (dom/form
   #js {:id "new-board-form"}
   (dom/div #js {:className "form-group" :key "new-board-form-div"}
            [(dom/input #js {:type "text"
                             :name "board-title"
                             :className "form-control"
                             :placeholder "Title"
                             :key "new-board-form-div-inp1"}
                        )
             (dom/input #js {:type "text"
                             :name "board-description"
                             :className "form-control"
                             :placeholder "Description"
                             :key "new-board-form-div-inp2"}
                        )])))



(def new-task-form
  (dom/form
   #js {:id "new-task-form"}
   (dom/div #js {:className "form-group" :key "new-task-form-div"}
            [(dom/input #js {:type "text"
                             :name "task-title"
                             :className "form-control"
                             :placeholder "Title"
                             :key "new-task-form-div-inp1"}
                        )])))

(def new-column-form
  (dom/form
   #js {:id "new-column-form"}
   (dom/div #js {:className "form-group" :key "new-column-form-div"}
            [(dom/input #js {:type "text"
                             :name "column-title"
                             :className "form-control"
                             :placeholder "Title"
                             :key "new-column-form-div-inp1"}
                        )])))

(def bs-modal (js/React.createFactory js/ReactBootstrap.Modal))
(def bs-button (js/React.createFactory js/ReactBootstrap.Button))
(def bs-modal-title (js/React.createFactory js/ReactBootstrap.Modal.Title))
(def bs-modal-header (js/React.createFactory js/ReactBootstrap.Modal.Header))
(def bs-modal-body (js/React.createFactory js/ReactBootstrap.Modal.Body))
(def bs-modal-footer (js/React.createFactory js/ReactBootstrap.Modal.Footer))

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
         save-stngs (cond-> {:bsStyle "primary"
                             :onClick #(submit-fn h-env)
                             :key "mod-btn2"}
                      (= :off save-btn-state) (assoc :disabled "disabled"))
         save (fn [b] (bs-button (clj->js save-stngs) b))]
     (bs-modal #js {:show show :onHide #(h/modal-close h-env)}
               [(bs-modal-header #js {:closeButton true :key "modal-header"}
                                 (bs-modal-title nil (dom/h4 #js {:key "new-board-save-h41"} title)))
                (bs-modal-body #js {:key "modal-body"} modal-content)
                (bs-modal-footer #js {:key "modal-footer"}
                                 [(close "Close")
                                  (save "Save")])]))))
