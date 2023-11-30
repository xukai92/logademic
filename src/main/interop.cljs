(ns interop
  (:require
   [cljs.core.async :refer [go <!]]
   [cljs.core.async.interop :refer-macros [<p!]]
   ["@logseq/libs"]))

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

(defn set-block-collapsed [& args]
  (go (<p! (apply js/logseq.Editor.setBlockCollapsed args))))

(defn get-editing-block-content []
  (go (<p! (js/logseq.Editor.getEditingBlockContent))))
