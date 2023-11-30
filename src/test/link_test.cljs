(ns link-test
  (:require
   [cljs.test :refer [async deftest is are] :refer-macros [run-tests]]
   [cljs.core.async :refer [go <!]]
   [link]))

(defn take-leading-chars [n s]
  (apply str (take n (seq s))))

(deftest main
  (are [in out] (= (link/host-of in) out)
    "https://arxiv.org/abs/2104.05134" "arxiv.org"
    "https://openreview.net/pdf?id=SJg7spEYDS" "openreview.net"
    "https://xuk.ai" "xuk.ai")

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

(deftest paper-info-of
  (async done
         (go (are [in out] (let [info (<! (link/paper-info-of in))]
                             (and (= (:title info) (:title out))
                                  (= (take-leading-chars 32 (:abstract info)) (:abstract-32-chars out))))
               "https://arxiv.org/abs/2104.05134" {:title "Couplings for Multinomial Hamiltonian Monte Carlo"
                                                   :abstract-32-chars "Hamiltonian Monte Carlo (HMC) is"}
               "https://openreview.net/forum?id=SJg7spEYDS" {:title "Generative Ratio Matching Networks"
                                                             :abstract-32-chars "Deep generative models can learn"})
             (done))))

(do (enable-console-print!)
    (run-tests))
