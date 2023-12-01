(ns chat-test
  (:require
   [cljs.test :refer [async deftest is are] :refer-macros [run-tests]]
   [cljs.core.async :refer [go <!]]
   [interop] [chat]))

(deftest main

  (async done
         (go (let [system-message (interop/setting-of "systemMessage")
                   system-message-markdown (chat/augment-system-message system-message "markdown")
                   system-message-org (chat/augment-system-message system-message "org")]
               (println system-message)
               (println system-message-markdown)
               (println system-message-org))
             (done))))

(do (enable-console-print!)
    (run-tests))
