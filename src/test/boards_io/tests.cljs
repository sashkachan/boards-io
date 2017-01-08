(ns boards-io.tests
  (:require
            [cljsjs.react]
            [clojure.test :refer [deftest is are testing run-tests]]
            [om.next :as om :refer-macros [defui ui]]))

(deftest test-empty
  (is (= 1 1)))


