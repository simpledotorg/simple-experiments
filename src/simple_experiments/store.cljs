(ns simple-experiments.store
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.reader :as reader]
            [clojure.string :as s]
            [simple-experiments.db.patient :as db-patient]
            [simple-experiments.db.seed :as db-seed]
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

(defn reset-to-seed-data! [_ _]
  (let [store-map {:patients (db-seed/patients-by-id)}]
    (persist! store-map)
    {:dispatch [:on-store-load store-map]}))

(defn init! []
  (reg-event-fx :on-store-load on-store-load)
  (reg-event-fx :persist-store persist-store)
  (reg-event-fx :reset-to-seed-data reset-to-seed-data!)
  (go
    (let [store-str (<! (fetch!))
          store-map (or (reader/read-string store-str)
                        {:patients (db-seed/patients-by-id)})]
      (persist! store-map)
      (dispatch [:on-store-load store-map]))))

(comment
  ;; clear store
  (persist! "")
  (dispatch [:on-store-load nil])
  ;; restart app to take effect!
  )
