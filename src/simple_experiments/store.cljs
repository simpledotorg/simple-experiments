(ns simple-experiments.store
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.reader :as reader]
            [clojure.string :as s]
            [re-frame.core :refer [reg-event-db reg-event-fx reg-fx after dispatch]]
            [cljs.core.async :refer [put! chan <! >! timeout close!]]))

(def ReactNative (js/require "react-native"))
(def async-storage (.-AsyncStorage ReactNative))

(defn fetch! []
  (let [ch (chan)]
    (-> (.getItem async-storage "store")
        (.then #(put! ch %))
        (.catch #(put! ch "")))
    ch))

(defn persist! [value]
  (-> (.setItem async-storage "store" (str value))
      (.then #(prn "Persisted store."))
      (.catch #(prn "Error persisting store"))))

(defn on-store-load [{:keys [db]} [_ store]]
  {:db (assoc db :store store)})

(defn persist-store [{:keys [db]} _]
  (persist! (:store db))
  {})

(defn init! []
  (reg-event-fx :on-store-load on-store-load)
  (reg-event-fx :persist-store persist-store)
  (go
    (let [store-str (<! (fetch!))
          store-map (reader/read-string store-str)]
      (persist! store-map)
      (dispatch [:on-store-load store-map]))))

(comment
  ;; clear store
  (persist! "")
  )