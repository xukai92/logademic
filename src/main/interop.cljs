(ns interop
  (:require
   [cljs.core.async :refer [chan put! go <!]]
   [cljs.core.async.interop :refer-macros [<p!]]
   ["@logseq/libs"]
   ["openai" :refer [OpenAI]]))

;; Logseq

(defn settings-of []
  (js->clj js/logseq.settings))

(defn setting-of [key]
  (get (settings-of) key))

(defn get-user-configs []
  (go (js->clj (<p! (js/logseq.App.getUserConfigs)))))

(defn get-user-format []
  (go (get (<! (get-user-configs)) "preferredFormat")))

(defn get-current-page []
  (go (<p! (js/logseq.Editor.getCurrentPage))))

(defn get-current-format []
  (go (aget (<! (get-current-page)) "format")))

(defn get-current-block [& args]
  (go (<p! (apply js/logseq.Editor.getCurrentBlock args))))

(defn update-block [& args]
  (go (<p! (apply js/logseq.Editor.updateBlock args))))

(defn insert-block [& args]
  (go (<p! (apply js/logseq.Editor.insertBlock args))))

(defn upsert-block-property [& args]
  (go (<p! (apply js/logseq.Editor.upsertBlockProperty args))))

(defn set-block-collapsed [& args]
  (go (<p! (apply js/logseq.Editor.setBlockCollapsed args))))

(defn get-editing-block-content []
  (go (<p! (js/logseq.Editor.getEditingBlockContent))))

;; OpenAI

(defn new-client [base-url api-key]
  (let [options {:baseURL base-url
                 :apiKey api-key
                 :dangerouslyAllowBrowser true}]
    (new OpenAI (clj->js options))))

(defn chat-completions-create [client messages model stream]
  (let [channel (chan)]
    (-> (.-chat client)
        (.-completions)
        (.create #js {:messages (clj->js messages)
                      :model model
                      :stream stream})
        (.then (fn [response]
                 (put! channel (js->clj response))))
        (.catch (fn [error]
                  (put! channel error))))
    channel))
