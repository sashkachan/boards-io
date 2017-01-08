(ns boards-io.run-tests
  (:require [cljs.test :refer-macros [run-tests]]
            [cljs.nodejs]
            [boards-io.tests]))

(enable-console-print!)

(defn main []
  (run-tests 'boards-io.tests))

(set! *main-cli-fn* main)
