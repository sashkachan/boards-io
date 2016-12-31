(ns boards-io.transit
  (:require [cognitect.transit :as t]
            [boards-io.router :as r]
            [clojure.string :as strr]
            [bidi.bidi :as b]
            [goog.log :as glog]
            [boards-io.logger :as l]
            [boards-io.handlers :as h]
            [om.next :as om])
  (:import [goog.net XhrIo]))

(defn transit-post [url]
  (fn [{:keys [remote]} cb]
    (let [{:keys [query rewrite]} (om/process-roots remote)]
      (.send XhrIo url
             (fn [e]
               (this-as this
                 (let [response (.getResponseText this)
                       inter (t/read (t/reader :json) response)
                       rewritten (rewrite inter)]
                   (cb rewritten))))
             "POST" (t/write (t/writer :json) query)
             #js {"Content-Type" "application/transit+json"}))))
