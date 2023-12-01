(ns interop-test
  (:require
   [cljs.test :refer [async deftest is are] :refer-macros [run-tests]]
   [cljs.core.async :refer [go <!]]
   [interop] [chat]))

(deftest logseq)

(deftest openai
  (async done
         (go (let [base-url (interop/setting-of "baseURL")
                   api-key (interop/setting-of "apiKey")]
              (when api-key
                (let [client (interop/new-client base-url api-key)
                      query "what's 1 + 1? give me the answer only."
                      messages [{:role "user" :content query}]
                      model (interop/setting-of "model")
                      stream (interop/setting-of "stream")
                      response (<! (interop/chat-completions-create client
                                                                    messages
                                                                    model
                                                                    stream))
                      message-content (chat/get-message-content response)] 
                  (is (= message-content "2")))
                #_(let [client (interop/new-client base-url api-key)
                      query "what's 1 + 1? give me the answer only."
                      messages [{:role "user" :content query}]
                      model (interop/setting-of "model")
                      response (<! (interop/chat-completions-create client
                                                                    messages
                                                                    model
                                                                    true))]
                  (println response))))
             (done))))

(do (enable-console-print!)
    (run-tests))
