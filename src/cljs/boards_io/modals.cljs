(ns boards-io.modals
  (:require [om.dom :as dom]
            [om.next :as om :refer-macros [defui ui]]
            [boards-io.handlers :as h]))

(def new-board-form
  (dom/form
   nil
   (dom/div #js {:className "form-group"}
            [(dom/input #js {:type "text"
                            :className "form-control"
                            :placeholder "Title"}
                        )
             (dom/input #js {:type "text"
                            :className "form-control"
                            :placeholder "Description"}
                       )])))

(defui NewBoardItemModal
  Object
  (render
   [this]
   (let [close (fn [b cl ar] (dom/button
                             #js {:type "button"
                                  :className cl
                                  :aria-label ar
                                  :onClick #(h/new-board-close
                                             {:reconciler (om/get-reconciler this)})}
                             (dom/span #js {:aria-hidden "true"} b)))
         save (dom/button #js {:type "button"
                               :className "btn btn-primary"} "Save")]
     (dom/div
      #js {:className "modal-dialog" :role "document"}
      (dom/div #js {:className "modal-content"}
               [(dom/div #js {:className "modal-header"}
                         [(close "×" "close" "Close")
                          (dom/h4 #js {:className "modal-title"} "Create new board:")])
                (dom/div #js {:className "modal-body"}
                         new-board-form)
                (dom/div #js {:className "modal-footer"}
                         [(close "Close" "btn btn-default" "")
                          save])])))))
