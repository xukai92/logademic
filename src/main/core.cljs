(ns core
  (:require
   [clojure.string :as string]
   [cljs.core.async :as async :refer [chan put! close! go <!]]
   ["@logseq/libs"]
   [interop] [link] [chat]
   [interop-test] [link-test] [chat-test]
   [cljs.test :refer-macros [run-tests]]))

(def dev-msg "dev log")

(def settings-schema
  [{:key "baseURL"
    :type "string"
    :title "API URL"
    :description "The URL for an OpenAI-compatible API (defaults to OpenAI's API)."
    :default nil}
   {:key "apiKey"
    :type "string"
    :title "API Key"
    :description "Authentication key; for OpenAI's, see https://platform.openai.com/api-keys."
    :default nil}
   {:key "model"
    :type "enum"
    :title "Model name"
    :description "The name of the model to use."
    :enumPicker "radio"
    :enumChoices ["gpt-3.5-turbo-1106" "gpt-4-1106-preview" "gpt-4-vision-preview"]
    :default "gpt-3.5-turbo-1106"}
   {:key "stream"
    :type "boolean"
    :title "Stream response"
    :description "Read response in stream mode?"
    :default false}
   {:key "maxStreamElapsed"
    :type "number"
    :title "Maximum stream wait time (s)"
    :description "The maximum time (in seconds) to wait when reading stream."
    :default 60}
   {:key "systemMessage"
    :type "string"
    :title "System message"
    :description "The system message to start the conversation."
    :default "You're a helpful & smart assistant. Please provide concise & correct answers."}
   {:key "autoNewBlock"
    :type "boolean"
    :title "Automatically add new block"
    :description "Automatically start in a new block after the bot response?"
    :default true}
   {:key "debugPrompts"
    :type "boolean"
    :title "Debug prompts"
    :description "Print prompts in console (CMD-OPT-I) for debugging purposes?"
    :default false}
   ;;
   {:key "userName"
    :type "string"
    :title "User name"
    :description "Your preferred name for Logademic to say hello."
    :default nil}])

(defn link-paper [uuid content include-rating]
  (go (let [raw-url (string/trim content)
            host (link/host-of raw-url)]
        (if (link/known-host? host)
          (let [url (link/ensure-web-url raw-url)
                paper-info (<! (link/paper-info-of url))
                current-format (<! (interop/get-current-format))
                user-format (<! (interop/get-user-format))
                format (if current-format current-format user-format)
                new-content (link/format-link (:title paper-info) url format)]
            (<! (interop/update-block uuid new-content))
            (<! (interop/insert-block uuid (:abstract paper-info) #js{:focus false}))
            (<! (interop/set-block-collapsed uuid #js{:flag true})))
          (println (str dev-msg " | unknown host " host))))))
(defn a-link [include-rating]
  (go (let [current-block (<! (interop/get-current-block))
            current-uuid (aget current-block "uuid")
            current-content (<! (interop/get-editing-block-content))]
        (link-paper current-uuid current-content include-rating))))
(defn a-links [include-rating]
  (go (let [current-block (<! (interop/get-current-block #js{:includeChildren true}))
            child-blcoks (aget current-block "children")]
        (doseq [child-block child-blcoks]
          (link-paper (aget child-block "uuid") (aget child-block "content") include-rating)))))

(defn chat-block [client messages model stream new-block]
  (when (interop/setting-of "debugPrompts")
    (println
     (str "---" "\n"
          (clojure.string/join "\n\n"
                               (map (fn [msg] (str (:role msg) ":\n" (:content msg))) messages))
          "\n" "---")))
  (go (let [response (interop/chat-completions-create client messages model stream)
            uuid (aget new-block "uuid")]
        (if stream
          (let [start (.now js/Date)
                max-stream-elapsed (interop/setting-of "maxStreamElapsed")]
            (loop [content (aget new-block "content")]
              (let [chunk (<! response)
                    elapsed (/ (- (.now js/Date) start) 1000)]
                (if (< elapsed max-stream-elapsed)
                  (let [delta-content (chat/get-delta-content chunk)
                        new-content (str content delta-content)
                        finish-reason (get-in chunk ["choices" 0 "finish_reason"])]
                    (<! (interop/update-block uuid new-content #js{:focus false}))
                    (when (nil? finish-reason)
                      (recur new-content)))
                  (js/logseq.App.showMsg "Time out when reading response stream!" "error")))))
          (let [content (aget new-block "content")
                message-content (chat/get-message-content (<! response))
                new-content (str content message-content)]
            (<! (interop/update-block uuid new-content #js{:focus false})))))))
(defn a-ask []
  (go (let [base-url (interop/setting-of "baseURL")
            api-key (interop/setting-of "apiKey")
            client (interop/new-client base-url api-key)
            system-message (interop/setting-of "systemMessage")
            current-format (<! (interop/get-current-format))
            user-format (<! (interop/get-user-format))
            format (if current-format current-format user-format)
            augmented-system-message (chat/augment-system-message system-message format)
            current-block (<! (interop/get-current-block))
            current-uuid (aget current-block "uuid")
            current-content (<! (interop/get-editing-block-content))
            messages [{:role "system" :content augmented-system-message}
                      {:role "user" :content current-content}]
            model (interop/setting-of "model")
            stream (interop/setting-of "stream")
            new-content (chat/prepend-property-str format model "\n")
            new-block (<! (interop/insert-block current-uuid new-content #js{:focus false}))]
        (<! (chat-block client messages model stream new-block))
        (when (interop/setting-of "autoNewBlock")
          (interop/insert-block current-uuid "" #js{:sibling true})))))
(defn a-chat []
  (go (let [base-url (interop/setting-of "baseURL")
            api-key (interop/setting-of "apiKey")
            client (interop/new-client base-url api-key)
            system-message (interop/setting-of "systemMessage")
            current-format (<! (interop/get-current-format))
            user-format (<! (interop/get-user-format))
            format (if current-format current-format user-format)
            augmented-system-message (chat/augment-system-message system-message format)
            current-block (<! (interop/get-current-block))
            current-uuid (aget current-block "uuid")
            current-content (<! (interop/get-editing-block-content))
            parent-id (aget (aget current-block "parent") "id")
            parent-block (<! (interop/get-block parent-id #js{:includeChildren true}))
            parent-content (aget parent-block "content")
            parent-messages [{:role "system" :content augmented-system-message}
                             {:role "user" :content parent-content}]
            child-messages (map #(chat/child-block-to-message % format current-uuid current-content)
                                (map js->clj 
                                     (aget parent-block "children")))
            messages (concat parent-messages child-messages)
            model (interop/setting-of "model")
            stream (interop/setting-of "stream")
            new-content (chat/prepend-property-str format model "\n")
            parent-uuid (aget parent-block "uuid")
            new-block (<! (interop/insert-block parent-uuid new-content #js{:focus false}))
            new-uuid (aget new-block "uuid")]
        (<! (chat-block client messages model stream new-block))
        (when (interop/setting-of "autoNewBlock")
          (interop/insert-block new-uuid "" #js{:sibling true})))))

(defn main []
  (js/logseq.useSettingsSchema (clj->js settings-schema))

  (js/logseq.Editor.registerSlashCommand
   "a-link" (fn [] (a-link false)))
  (js/logseq.Editor.registerSlashCommand
   "a-links" (fn [] (a-links false)))

  (js/logseq.Editor.registerSlashCommand
   "a-ask" a-ask)
  (js/logseq.Editor.registerSlashCommand
   "a-chat" a-chat)

  (js/logseq.Editor.registerSlashCommand
   "a-dev" (fn []
             (go
               (let [current-block (<! (interop/get-current-block))
                     current-uuid (aget current-block "uuid")]
                 (println (<! (interop/get-block-properties current-uuid)))
                 (println (<! (interop/get-block-property current-uuid "chatseq-model")))))))

  (let [user-name (interop/setting-of "userName")]
    (js/logseq.App.showMsg (if (empty? user-name)
                             "Hello from Logademic!"
                             (str "Hello " user-name "---Greeting from Logacademic!"))))

  (enable-console-print!)
  (run-tests 'interop-test)
  (run-tests 'link-test)
  (run-tests 'chat-test))

(defn init []
  (-> (js/logseq.ready main)
      (.catch js/console.error)))
