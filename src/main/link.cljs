(ns link
  (:require [clojure.string :as string]
            [lambdaisland.uri :refer [uri]]
            [cljs.core.async :refer [go <!]]
            [cljs-http.client :as client]
            [hickory.core :refer [parse as-hickory]]
            [hickory.select :refer [tag select]]))

(defn host-of [url]
  (:host (uri url)))

(defn replacement-str-for [host]
  (cond
    (= host "arxiv.org") "abs"
    (= host "openreview.net") "forum" 
    :else nil))

(defn ensure-web-url [url]
  (let [ends-with-dot-pdf (string/ends-with? url ".pdf")
        url-without-dot-pdf (if ends-with-dot-pdf
                              (subs url 0 (- (count url) 4))
                              url)
        replacement-str (replacement-str-for (host-of url))]
    (if (nil? replacement-str)
      url-without-dot-pdf
      (string/replace url-without-dot-pdf #"pdf" replacement-str))))

;; get the DOM tree of the fetched HTTP response given URL
(defn get-dom-tree [url]
  (go
    (-> (<! (client/get url))
        :body
        parse
        as-hickory)))

;; get the title content
(defn title-content-of [dom-tree]
  (-> (select (tag :title) dom-tree)
      first
      :content
      first))

;; "XXX | OpenReview" -> "XXX"
;; "[1234.56789] XXX" -> "XXX"
(defn clean-title-content [host title-content]
  (cond
    (= host "arxiv.org") (string/replace title-content #"^\[(.*?)\]\s" "")
    (= host "openreview.net") (string/replace title-content #"\s(\|\sOpenReview)$" "")
    :else nil))

(defn paper-title-of [raw-url]
  (go
    (let [host (host-of raw-url)
          url (ensure-web-url raw-url)
          dom-tree (<! (get-dom-tree url))
          title-content (title-content-of dom-tree)]
      (clean-title-content host title-content))))

(defn format-link [title link format]
  (assert (contains? #{"markdown" "org"} format))
    (cond
        (= format "markdown") (str "[" title "](" link ")")
        (= format "org") (str "[[" link "][" title "]]")))

;; dev
(defn dev [] (go))
