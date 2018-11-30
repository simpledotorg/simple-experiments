(ns simple-experiments.events.simple-card
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [cljs-time.format :as timef]
            [clojure.string :as string]
            [simple-experiments.db :as db :refer [app-db]]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.events.utils :as u :refer [assoc-into-db]]))

(defn close-association-confirmation [{:keys [db]} _]
  {:db (assoc-in db [:ui :summary :show-association-confirmation] false)})

(defn show-association-confirmation [{:keys [db]} _]
  {:db (assoc-in db [:ui :summary :show-association-confirmation] true)})

(defn associate-simple-card-with-patient [{:keys [db]} [_ card-uuid patient-id]]
  {:db (-> db
           (update-in [:store :patients patient-id :card-uuids] conj card-uuid)
           (assoc-in [:ui :active-card :status] :associated))})

(defn pending? [active-card]
  (and (some? active-card)
       (#{:awaiting-association :awaiting-registration} (:status active-card))))

(defn awaiting-association? [active-card]
  (and (some? active-card)
       (#{:awaiting-association} (:status active-card))))

(defn register-events []
  (reg-event-fx :close-association-confirmation close-association-confirmation)
  (reg-event-fx :show-association-confirmation show-association-confirmation)
  (reg-event-fx :associate-simple-card-with-patient associate-simple-card-with-patient))
