(ns chat
  (:require [clojure.string :as string]))

(def format-instruction-markdown
  "Please assist by reading and responding in Markdown syntax used by Logseq's blocks, with the following additional notes:
* Use `*` for lists instead of `-`.
* Avoid using headings like `#`, `##`, etc.
* Avoid nesting lists.
* Avoid using sub-items.")

(def format-instruction-org
  "Please assist by reading and responding in Org mode syntax used by Logseq's blocks, with the following additional notes:
- Markup examples:
  #+BEGIN_SRC org
  *bold*, =verbatim=, /italic/, +strikethrough+, _underline_, ~code~, [[protocal://some.domain][some label]]
  #+END_SRC
- Note that bold uses single ~*~ to quote,, i.e. ~*bold*~ instead of ~**bold**~.
- Avoid using headings.
- Avoid nesting lists.
* Avoid using sub-items.
- Avoid quoting the entire response in a greater block.")

(defn augment-system-message [system-message format]
  (let [format-instruction (cond
                             (= format "markdown") format-instruction-markdown
                             (= format "org") format-instruction-org
                             :else nil)]
    (if format-instruction
      (str system-message " " format-instruction)
      format-instruction)))

(defn prepend-property-str [format model s]
  (let [property-str (cond
                       (= format "markdown") (str "chatseq-model::" " " model)
                       (= format "org") (str ":PROPERTIES:" "\n" ":chatseq-model:" " " model "\n" ":END:")
                       :else nil)]
    (str property-str s)))

(defn remove-property-str [format s]
  (let [pattern (cond
                  (= format "markdown") #"^chatseq-model\:\: .+\n"
                  (= format "org") #"^\:PROPERTIES\:\n\:chatseq-model\: .+\n\:END\:\n"
                  :else nil)]
    (if pattern (string/replace s pattern "") s)))

(defn get-message-content [response]
  (get-in response ["choices" 0 "message" "content"]))

(defn get-delta-content [chunk]
  (get-in chunk ["choices" 0 "delta" "content"]))

(defn child-block-to-message [child-block format current-uuid current-content]
  (let [uuid (get child-block "uuid")
        content (get child-block "content")
        property-value (get-in child-block ["properties" "chatseqModel"])]
    (if property-value
      {:role "assistant" :content (chat/remove-property-str format content)}
      (if (= uuid current-uuid)
        {:role "user" :content current-content}
        {:role "user" :content content}))))
