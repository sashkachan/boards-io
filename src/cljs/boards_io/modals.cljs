(ns boards-io.modals
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui ui]]
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

(defui Modal
  Object
  (render
   [this]
   (let [{:keys [save-btn-state ref modal-content submit-fn title extras] :as props} (om/props this)
         h-env {:reconciler (om/get-reconciler this)
                :save-btn-field :board/save-btn-field
                :ref ref
                :extras extras}
         close (fn [b cl ar] (dom/button
                             #js {:type "button"
                                  :className cl
                                  :aria-label ar
                                  :key "mod-btn1"
                                  :onClick #(h/modal-close h-env)}
                             (dom/span #js {:aria-hidden "true" :key "mod-spn-1"} b)))
         save-stngs (-> (cond-> {:type "button"
                                 :className "btn btn-primary"
                                 :onClick #(submit-fn h-env)
                                 :key "mod-btn2"}
                          (= :off save-btn-state) (assoc :disabled "disabled"))
                        clj->js)
         save (dom/button save-stngs  "Save")]
     (dom/div #js {:id "modal-wrap"}
              (dom/div #js {:className "modal fade in"
                            :style #js {:display "visible"} }
                       (dom/div
                        #js {:className "modal-dialog" :role "document" :id "new-board-save" :key "new-board-save"}
                        (dom/div #js {:className "modal-content" :key "new-board-save-div1"}
                                 [(dom/div #js {:className "modal-header" :key "new-board-save-div11" }
                                           [(close "×" "close" "Close")
                                            (dom/h4 #js {:className "modal-title" :key "new-board-save-h41"} title)])
                                  (dom/div #js {:className "modal-body" :key "new-board-save-div12"}
                                           modal-content)
                                  (dom/div #js {:className "modal-footer" :key "new-board-save-div13"}
                                           [(close "Close" "btn btn-default" "")
                                            save])]))))

     )))
