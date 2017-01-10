(ns boards-io.run-tests
  (:require [cljs.test :refer-macros [run-tests]]
            [cljs.nodejs :as nodejs]
            [boards-io.tests]))

(nodejs/enable-util-print!)

(defn main []
  (run-tests 'boards-io.tests))

(set! *main-cli-fn* main)
