(ns boards-io.core
  (:gen-class)
  (:require 
   [boards-io.system :as s]))

(defn -main []
  (s/system-start))


;; system 
