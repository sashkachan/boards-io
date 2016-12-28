(ns boards-io.core
  (:require 
   [boards-io.system :as s]
   [figwheel-sidecar.repl-api :as ra]
   [datomic.api :as d]
   [hiccup.core :as hc]))

(defn -main []
  (s/system-start))


;; system 
