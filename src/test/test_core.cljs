(ns test-core
  (:require
   [cljs.test :refer [deftest is are]]
   [core]))

(deftest test-plus-inc
  (is (= (+ 1 1) 2))
  (are [in out] (= (inc in) out)
    1 2
    3 4))
