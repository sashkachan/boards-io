(ns boards-io.modals
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui ui]]
            [boards-io.handlers :as h]))

(def new-board-form
  (dom/form
   #js {:id "new-board-form"}
   (dom/div #js {:className "form-group"}
            [(dom/input #js {:type "text"
                             :name "board-title"
                             :className "form-control"
                             :placeholder "Title"}
                        )
             (dom/input #js {:type "text"
                             :name "board-description"
                             :className "form-control"
                             :placeholder "Description"}
                        )])))

(defui NewBoardItemModal
  Object
  (render
   [this]
   (let [h-env {:reconciler (om/get-reconciler this)}
         close (fn [b cl ar] (dom/button
                             #js {:type "button"
                                  :className cl
                                  :aria-label ar
                                  :onClick #(h/modal-close h-env :board/new-board-modal)}
                             (dom/span #js {:aria-hidden "true"} b)))
         save (dom/button #js {:type "button"
                               :className "btn btn-primary"
                               :onClick #(h/new-board-save
                                          h-env)} "Save")]
     (dom/div
      #js {:className "modal-dialog" :role "document" :id "new-board-save"}
      (dom/div #js {:className "modal-content"}
               [(dom/div #js {:className "modal-header"}
                         [(close "×" "close" "Close")
                          (dom/h4 #js {:className "modal-title"} "Create new board:")])
                (dom/div #js {:className "modal-body"}
                         new-board-form)
                (dom/div #js {:className "modal-footer"}
                         [(close "Close" "btn btn-default" "")
                          save])])))))
