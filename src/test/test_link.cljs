(ns test-link
  (:require
   [cljs.test :refer [deftest is are]]
   [link]))

(deftest test-all
  (are [in out] (= (link/host-of in) out)
    "https://arxiv.org/abs/2104.05134" "arxiv.org"
    "https://openreview.net/pdf?id=SJg7spEYDS" "openreview.net"
    "https://blog.xuk.ai" "xuk.ai")
  (are [in out] (= (link/replacement-str-for in) out)
    "arxiv.org" "abs"
    "openreview.net" "forum"
    "xuk.ai" nil)
  (is (= (link/ensure-web-url "https://arxiv.org/pdf/2104.05134.pdf") "https://arxiv.org/abs/2104.05134"))
  (are [in out] (= (link/ensure-web-url in) out)
    "https://arxiv.org/abs/2104.05134" "https://arxiv.org/abs/2104.05134"
    "https://arxiv.org/pdf/2104.05134.pdf" "https://arxiv.org/abs/2104.05134"
    "https://openreview.net/pdf?id=SJg7spEYDS" "https://openreview.net/forum?id=SJg7spEYDS"
    "https://openreview.net/forum?id=SJg7spEYDS" "https://openreview.net/forum?id=SJg7spEYDS"))
