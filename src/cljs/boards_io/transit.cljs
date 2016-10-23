(ns boards-io.transit
  (:require [cognitect.transit :as t]
            [boards-io.router :as r]
            [clojure.string :as strr]
            [bidi.bidi :as b]
            [om.next :as om])
  (:import [goog.net XhrIo]))

(defn transit-post [url]
  (fn [{:keys [remote]} cb]
    (let [{:keys [query rewrite]} (om/process-roots remote)]
      (println "send: " remote " meta " (meta (-> remote first :boards)) "query "query)
      (.send XhrIo url
             (fn [e]
               (this-as this
                 (.log js/console (.getResponseText this))
                 (let [response (.getResponseText this)
                       inter (t/read (t/reader :json) response)
                       _ (println "inter " inter)
                       rewritten (rewrite inter)
                       _ (println "rewritten " rewritten)
                       ]
                   (cb rewritten))))
             "POST" (t/write (t/writer :json) query)
             #js {"Content-Type" "application/transit+json"}))))
