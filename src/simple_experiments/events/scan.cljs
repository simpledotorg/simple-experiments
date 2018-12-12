(ns simple-experiments.events.scan
  (:require [re-frame.core :refer [reg-event-fx]]
            [simple-experiments.events.navigation :as nav]))

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
    (case (nav/previous-screen)
      :home
      (if (nil? existing-patient)
        {:dispatch-n [[:goto :patient-search]
                      [:patient-search-clear]
                      [:goto-search-mode]
                      [:set-active-card card-uuid nil :pending]]}
        {:dispatch-n [[:set-active-patient-id (:id existing-patient)]
                      [:show-bp-sheet]
                      [:set-active-card card-uuid nil :associated]]})

      :new-patient
      {:dispatch-n [[:set-active-card card-uuid nil :pending-registration]
                    [:go-back]]}

      {})))

(defn register-events []
  (reg-event-fx :show-camera show-camera)
  (reg-event-fx :hide-camera hide-camera)
  (reg-event-fx :handle-scan handle-scan))
