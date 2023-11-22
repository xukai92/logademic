(ns core
  (:require
   ["@logseq/libs"]))

(def plugin-settings
  [{:key "userName"
    :type "string"
    :title ""
    :description ""
    :default nil}])

(defn main []
  (js/logseq.useSettingsSchema (clj->js plugin-settings))
  (let [user-name js/logseq.settings.userName]
    (js/logseq.App.showMsg (if (empty? user-name)
                             "Hello from Logacademic!"
                             (str "Hello " user-name "---Greeting from Logacademic!")))))

(defn init []
  (-> (js/logseq.ready main)
      (.catch js/console.error)))
