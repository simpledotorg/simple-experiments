(ns simple-experiments.store
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.reader :as reader]
            [clojure.string :as s]
            [simple-experiments.db.patient :as db-patient]
            [simple-experiments.db.seed :as db-seed]
            [simple-experiments.db.seed.data :as db-seed-data]
            [simple-experiments.events.settings :as settings]
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
  {:db (-> db
           (assoc :store store)
           (assoc :active-page (keyword (get-in store [:settings :start-screen] :home))))})

(defn persist-store [{:keys [db]} _]
  (persist! (:store db))
  {})

(defn set-seed-state-and-district [db [_ state district]]
  (-> db
      (assoc-in [:seed :state] state)
      (assoc-in [:seed :district] district)))

(defn reset-to-seed-data! [{:keys [db]} _]
  (let [store-map {:patients (db-seed/patients-by-id db)
                   :settings settings/default-settings}]
    (persist! store-map)
    {:dispatch [:on-store-load store-map]}))

(defn init! []
  (reg-event-fx :on-store-load on-store-load)
  (reg-event-fx :persist-store persist-store)
  (reg-event-fx :reset-to-seed-data reset-to-seed-data!)
  (reg-event-db :set-seed-state-and-district set-seed-state-and-district)
  (go
    (let [store-str (<! (fetch!))
          store-map (or (reader/read-string store-str)
                        {:patients (db-seed/patients-by-id)
                         :settings settings/default-settings})]
      (persist! store-map)
      (dispatch [:on-store-load store-map])
      (dispatch [:set-seed-state-and-district
                 (:state db-seed-data/patients)
                 (:district db-seed-data/patients)]))))

(comment
  ;; clear store
  (persist! "")
  (dispatch [:on-store-load nil])
  ;; restart app to take effect!
  )
