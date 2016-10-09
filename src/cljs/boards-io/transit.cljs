(ns boards-io.transit
  (:require [cognitect.transit :as t]
            [boards-io.router :as r]
            [clojure.string :as strr]
            [bidi.bidi :as b])
  (:import [goog.net XhrIo]))

(defn transit-post [url]
  (fn [{:keys [remote]} cb]
    (.send XhrIo url
           (fn [e]
             (this-as this
               (.log js/console (.getResponseText this))
              (cb (t/read (t/reader :json) (.getResponseText this)))))
           "POST" (t/write (t/writer :json) remote)
           #js {"Content-Type" "application/transit+json"})))
