(ns core
  (:require
   [clojure.string :as string]
   [cljs.core.async :refer [go <!]]
   ["@logseq/libs"]
   [interop-test] [link-test]
   [interop] [link]))

(def dev-msg "dev log")

(def plugin-settings
  [{:key "userName"
    :type "string"
    :title ""
    :description ""
    :default nil}])

(defn update-paper-link [uuid content]
  (go (let [raw-url (string/trim content)
            host (link/host-of raw-url)]
        (if (link/known-host? host)
          (let [url (link/ensure-web-url raw-url)
                paper-info (<! (link/paper-info-of url))
                format (<! (interop/get-current-format))
                new-content (link/format-link (:title paper-info) url format)]
            (<! (interop/update-block uuid new-content))
            (<! (interop/insert-block uuid (:abstract paper-info) #js{:focus false}))
            (<! (interop/set-block-collapsed uuid #js{:flag true})))
          (println (str dev-msg " | unknown host " host))))))

(defn main []
  (js/logseq.useSettingsSchema (clj->js plugin-settings))
  (js/logseq.Editor.registerSlashCommand
   "alink" (fn []
             (go
               (let [current-block (<! (interop/get-current-block))
                     current-uuid (aget current-block "uuid")
                     current-content (<! (interop/get-editing-block-content))]
                 (update-paper-link current-uuid current-content)))))
  (js/logseq.Editor.registerSlashCommand
   "alinks" (fn []
              (go
                (let [current-block (<! (interop/get-current-block #js{:includeChildren true}))
                      child-blcoks (aget current-block "children")]
                  (doseq [child-block child-blcoks]
                    (update-paper-link (aget child-block "uuid") (aget child-block "content")))))))
  (let [user-name js/logseq.settings.userName]
    (js/logseq.App.showMsg (if (empty? user-name)
                             "Hello from Logademic!"
                             (str "Hello " user-name "---Greeting from Logacademic!")))))

(defn init []
  (-> (js/logseq.ready main)
      (.catch js/console.error)))
