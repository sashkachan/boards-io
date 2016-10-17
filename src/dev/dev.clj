(ns dev
  (:require [clojure.tools.namespace.repl :as nmr]
            [boards-io.system :as s]
            [figwheel-sidecar.repl-api :as ra]))

(defn system-restart []
  (s/system-stop)
  (nmr/refresh :after 'boards-io.system/system-start))


