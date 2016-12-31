(ns boards-io.logger
  (:require [goog.log :as glog])
  (:import [goog.debug Console]))

(defonce *logger*
  (when ^boolean goog.DEBUG
    (.setCapturing (Console.) true)
    (glog/getLogger "boards.io")))

