(ns link
  (:require [clojure.string :as string]
            [lambdaisland.uri :refer [uri]]
            [cljs.core.async :refer [go <!]]
            [cljs-http.client :as client]
            [hickory.core :refer [parse as-hickory]]
            [hickory.render :refer [hickory-to-html]]
            [hickory.select :as s]))

(defn host-of [url]
  (:host (uri url)))

(defn known-host? [host]
  (some #(= host %) ["arxiv.org" "openreview.net"]))

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
  (go (-> (<! (client/get url))
          :body
          parse
          as-hickory)))

;; get the title content
(defn title-content-of [dom-tree]
  (-> (s/select (s/tag :title) dom-tree)
      first
      :content
      first))

;; get the title content
(defn abstract-content-of [host dom-tree]
  (cond
    (= host "arxiv.org") (-> (s/select (s/class :abstract) dom-tree)
                             first
                             :content
                             (nth 2)
                             hickory-to-html)
    (= host "openreview.net") (-> (s/select (s/and (s/tag :meta) 
                                                   (s/attr :name #(= "citation_abstract" % ))) dom-tree)
                                  first
                                  :attrs
                                  :content
                                  hickory-to-html)
    :else nil))

;; "XXX | OpenReview" -> "XXX"
;; "[1234.56789] XXX" -> "XXX"
(defn clean-title-content [host title-content]
  (cond
    (= host "arxiv.org") (string/replace title-content #"^\[(.*?)\]\s" "")
    (= host "openreview.net") (string/replace title-content #"\s(\|\sOpenReview)$" "")
    :else nil))

(defn paper-info-of [url]
  (go (let [host (host-of url)
            dom-tree (<! (get-dom-tree url))
            title-content (title-content-of dom-tree)
            title (clean-title-content host title-content)
            abstract (abstract-content-of host dom-tree)]
        {:title title :abstract abstract})))

(defn known-format? [format]
  ;;(some #{format} ["markdown" "org"])) ;; alternative way that uses set as function
  (some #(= format %) ["markdown" "org"]))

(defn format-link [title link format]
  (assert (known-format? format))
  (cond
    (= format "markdown") (str "[" title "](" link ")")
    (= format "org") (str "[[" link "][" title "]]")))
