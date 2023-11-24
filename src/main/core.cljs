(ns core
  (:require
   [clojure.string :as string]
   [cljs.core.async :refer [go <!]]
   ["@logseq/libs"]
   [interop] [link]))

(def plugin-settings
  [{:key "userName"
    :type "string"
    :title ""
    :description ""
    :default nil}])

(defn update-paper-link [uuid content]
  (go
    (let [url (string/trim content)
          paper-title (<! (link/paper-title-of url))
          format (<! (interop/get-current-format))
          new-content (link/format-link paper-title url format)]
      (<! (interop/update-block uuid new-content)))))

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
                             "Hello from Logacademic!"
                             (str "Hello " user-name "---Greeting from Logacademic!"))))
  ;; dev funcs
  (let [dev-msg "dev log"]
    (go
      (println (str dev-msg " | 0"))
      (<! (interop/dev))
      (<! (link/dev))
      (println (str dev-msg " | 1")))))

(defn init []
  (-> (js/logseq.ready main)
      (.catch js/console.error)))
