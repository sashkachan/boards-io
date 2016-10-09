(ns boards-io.navigation
  (:require [goog.events :as events]
            [cljs.spec :as s])
  (:import [goog.history Html5History EventType]
           [goog.history.Html5History TokenTransformer]
           [goog.events EventType]
           [goog Uri]))

(defn- recur-href
  "Traverses up the DOM tree and returns the first node that contains a href attr"
  [target]
  (if (.-href target)
    target
    (when (.-parentNode target)
      (recur-href (.-parentNode target)))))


(defn- processable-url? [uri]
  (and (not (clojure.string/blank? uri))
       (or (and (not (.hasScheme uri)) (not (.hasDomain uri)))
           (some? (re-matches (re-pattern (str "^" (.-origin js/location) ".*$"))
                              (str uri))))))

(defn- get-token-from-uri [uri]
  (let [path (.getPath uri)
        query (.getQuery uri)]
    ;; Include query string in token
    (if (empty? query) path (str path "?" query))))

(defn- set-retrieve-token! [t]
  (set! (.. t -retrieveToken)
        (fn [path-prefix location]
          (let [tkn (str (.-pathname location) (.-search location))]
            (.log js/console tkn)
            tkn)))
  t)

(defn- set-create-url! [t]
  (set! (.. t -createUrl)
        (fn [token path-prefix location]
            (str path-prefix token)
  ))
  t)

(defn new-history
  ([]
   (new-history (-> (TokenTransformer.) set-retrieve-token! set-create-url!)))
  ([transformer]
   (doto (Html5History. js/window transformer)
     (.setUseFragment false)
     (.setPathPrefix "")
     (.setEnabled true))))

(defonce events-state (atom []))
(defonce history-state (atom nil))

(defn wire-up [hstr dispatch-fn]
  (let [_ (swap! events-state
                    conj
                    (goog.events/listen
                     hstr
                     goog.history.EventType.NAVIGATE
                     (fn [e]
                       (.log js/console "isNav " (.-isNavigation e))
                       (dispatch-fn (.-token e)))))]
    (dispatch-fn (.getToken hstr))
    (swap! events-state
           conj
           (events/listen
            js/window goog.events.EventType.CLICK
            (fn [e]
              (if-let [href (recur-href (.-target e))]
                (let [uri (.parse Uri href)] ;; shameless steal from pushy, like the rest of this file
                  (when (and (processable-url? uri)
                           (not (get #{"_blank" "_self"} (.-target href)))
                           (not= 1 (.-button e))
                           (not (.-altKey e))
                           (not (.-ctrlKey e))
                           (not (.-metaKey e))
                           (not (.-shiftKey e)))
                    (if-let [title (-> href .-title)]
                      (.setToken hstr (get-token-from-uri uri) title)
                      (.setToken hstr (get-token-from-uri uri)))     
                    (.preventDefault e)))))))))

(defn wind-down []
  (doseq [e @events-state]
    (events/unlistenByKey e))
  (reset! events-state nil)
  (reset! history-state nil))
