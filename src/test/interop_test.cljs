(ns interop-test
  (:require
   [cljs.test :refer [async deftest is are] :refer-macros [run-tests]]
   [cljs.core.async :refer [go <!]]
   [interop]))

(deftest main
  (async done
         (go (let [x (str "a")]
               (is (some #(= x %) ["a" "b"]))))
         (done)))

(do (enable-console-print!)
    (run-tests))
