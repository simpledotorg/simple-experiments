(ns simple-experiments.events.scan
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [cljs-time.format :as timef]
            [clojure.string :as string]
            [simple-experiments.db :as db :refer [app-db]]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.events.utils :as u :refer [assoc-into-db]]))

(defn show-camera [{:keys [db]} _]
  {:db (assoc-in db [:home :show-camera?] true)})

(defn hide-camera [{:keys [db]} _]
  {:db (assoc-in db [:home :show-camera?] false)})

(defonce parse-string
  (.-parseString (js/require "react-native-xml2js")))

(def id-fields
  #{:full-name :age :gender})

(defn find-patient [db card-uuid]
  (let [patients (vals (get-in db [:store :patients]))]
    (->> patients
         (filter #(contains? (:card-uuids %) card-uuid))
         first)))

(defn handle-scan [{:keys [db]} [_ event]]
  (let [card-uuid (uuid (:data (js->clj event :keywordize-keys true)))
        existing-patient (find-patient db card-uuid)]
    (if (nil? existing-patient)
      {:dispatch-n [[:goto :patient-list]
                    [:patient-search-clear]
                    [:goto-search-mode]
                    [:set-active-card card-uuid]]}
      {:dispatch-n [[:set-active-patient-id (:id existing-patient)]
                    [:show-bp-sheet]
                    [:set-active-card card-uuid]]})))

(defn register-events []
  (reg-event-fx :show-camera show-camera)
  (reg-event-fx :hide-camera hide-camera)
  (reg-event-fx :handle-scan handle-scan))
