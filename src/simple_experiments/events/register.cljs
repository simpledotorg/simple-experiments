(ns simple-experiments.events.register
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [simple-experiments.view.components :as c]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.db :as db :refer [app-db]]
            [simple-experiments.events.form :as f]
            [simple-experiments.events.utils :as u :refer [assoc-into-db]]))

(defn show-schedule-sheet [{:keys [db]} _]
  {:db (assoc-in db [:ui :summary :show-schedule-sheet?] true)
   :dispatch [:schedule-next-visit 30]})

(defn hide-schedule-sheet [db _]
  (assoc-in db [:ui :summary :show-schedule-sheet?] false))

(defn next-visit-time [days]
  (cond
    (= :none days)
    nil

    (nil? days)
    (timec/to-long (time/plus (time/now) (time/days 30)))

    :else
    (timec/to-long (time/plus (time/now) (time/days days)))))

(defn schedule-next-visit [{:keys [db]} [_ days]]
  {:db
   (-> db
       (assoc-in [:store :patients (u/active-patient-id db) :next-visit]
                 (next-visit-time days))
       (assoc-in [:ui :summary :next-visit] (or days 30)))
   :dispatch [:persist-store]})

(defn summary-save [{:keys [db]} _]
  {:db       (assoc-in db [:ui :summary] nil)
   :dispatch [:reset-to-home]})

(defn init-new-patient [{:keys [db]} [_ patient-id]]
  (let [patient (select-keys (get-in db [:ui :patient-search])
                             [:full-name :age :date-of-birth])]
    {:db (-> db
             (assoc-in [:ui :patient-form :values] patient)
             (assoc-in [:ui :patient-form :submit-action] [:register-new-patient]))}))

(defn register-new-patient [{:keys [db]} _]
  (if (get-in db [:ui :patient-form :valid?])
    (let [active-card (-> db :ui :active-card)
          patient (let [patient (f/patient-from-form (get-in db [:ui :patient-form]))]
                    (if-let [{:keys [uuid six-digit-id]} active-card]
                      (assoc patient
                             (if uuid :card-uuids :six-digit-ids)
                             #{(or uuid six-digit-id)})
                      patient))
          registration-complete-events [[:persist-store]
                                        [:set-active-patient-id (:id patient)]
                                        [:show-bp-sheet]
                                        [:update-active-card-status :associated]]]
      {:db (assoc-in db [:store :patients (:id patient)] patient)
       :dispatch-n registration-complete-events})
    {:db (assoc-in db [:ui :new-patient :show-errors?] true)}))

(defn register-events []
  (reg-event-fx :show-schedule-sheet show-schedule-sheet)
  (reg-event-db :hide-schedule-sheet hide-schedule-sheet)
  (reg-event-fx :init-new-patient init-new-patient)
  (reg-event-fx :register-new-patient register-new-patient)
  (reg-event-fx :schedule-next-visit schedule-next-visit)
  (reg-event-db :compute-errors compute-errors)
  (reg-event-fx :summary-save summary-save))
