(ns simple-experiments.events.update-patient
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [simple-experiments.view.components :as c]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.db :as db :refer [app-db]]
            [simple-experiments.events.form :as f]
            [simple-experiments.events.utils :as u :refer [assoc-into-db]]))

(defn init-edit-patient [{:keys [db]} [_ patient-id]]
  (let [patient (get-in db [:store :patients patient-id])]
    {:db (-> db
             (assoc-in [:ui :patient-form :values] patient)
             (assoc-in [:ui :patient-form :submit-action] [:update-patient]))}))

(defn update-patient [{:keys [db]} _]
  (if (get-in db [:ui :patient-form :valid?])
    (let [active-card (-> db :ui :active-card)
          patient (let [patient (f/patient-from-form (get-in db [:ui :patient-form]))]
                    (if-let [{:keys [uuid six-digit-id]} active-card]
                      (update patient
                              (if uuid :card-uuids :six-digit-ids)
                              set/union
                              #{(or uuid six-digit-id)})
                      patient))
          edit-complete-events [[:persist-store]
                                [:update-active-card-status :associated]
                                [:go-back]]]
      {:db (update-in db [:store :patients (:id patient)] merge patient)
       :dispatch-n edit-complete-events})
    {:db (assoc-in db [:ui :patient-form :show-errors?] true)}))

(defn register-events []
  (reg-event-fx :init-edit-patient init-edit-patient)
  (reg-event-fx :update-patient update-patient))
