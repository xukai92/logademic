(ns chat)

(def format-instruction-markdown
  "Please assist by reading and responding in Markdown syntax used by Logseq's blocks, with the following additional notes:
* Use `*` for lists instead of `-`.
* Do NOT include headings.
* Do NOT use nested lists and sub-items by avoding itemizing top-level texts.")

(def format-instruction-org
  "Please assist by reading and responding in Org mode syntax used by Logseq's blocks, with the following additional notes:
- Markup examples:
  #+BEGIN_SRC org
  *bold*, =verbatim=, /italic/, +strikethrough+, _underline_, ~code~, [[protocal://some.domain][some label]]
  #+END_SRC
- Note that bold uses single ~*~ to quote,, i.e. ~*bold*~ instead of ~**bold**~.
- Do NOT include headings.
- Do NOT use nested lists and sub-items by avoding itemizing top-level texts.
- Do NOT quote the entire response in a greater block.")

(defn augment-system-message [system-message format]
  (let [format-instruction (cond
                             (= format "markdown") format-instruction-markdown
                             (= format "org") format-instruction-org
                             :else nil)]
    (if format-instruction
      (str system-message " " format-instruction)
      format-instruction)))

(defn make-property-str [format model]
  (cond
    (= format "markdown") (str "chatseq-model::" " " model)
    (= format "org") (str ":PROPERTIES:" "\n" ":chatseq-model:" " " model "\n" ":END:")
    :else nil))

(defn get-message-content [response]
  (-> response
      (get "choices")
      first
      (get "message")
      (get "content"))
  (get-in response ["choices" 0 "message" "content"]))