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
            [simple-experiments.events.utils :as u :refer [assoc-into-db]]))

(def required
  {:spec ::db-p/non-empty-string :error "is required"})

(def all-validations
  {:full-name [{:spec ::db-p/non-empty-string :error "Please enter patient's full name."}]
   :birth-year [{:spec ::db-p/non-empty-string :error "Please enter patient's birth-year."}
                {:spec ::db-p/birth-year-string :error "Please enter a valid birth-year."}]
   :gender [{:spec ::db-p/non-empty-string :error "Please select a gender."}
            {:spec ::db-p/gender :error "Please enter a valid gender."}]
   :phone-number [{:spec ::db-p/non-empty-string :error "Please enter a phone number."}
                  {:spec ::db-p/phone-number :error "Please enter a valid phone number."}]
   :village-or-colony [{:spec ::db-p/non-empty-string :error "Please enter a village or colony."}]})

(defn patient-with-all-fields [patient]
  (->> db-p/patient-fields
       (map (fn [f] [f (patient f)]))
       (into {})))

(defn new-patient [db]
  (-> (get-in db [:ui :new-patient :values])
      patient-with-all-fields
      (merge {:id (str (random-uuid))})))

(defn first-error [field-name field-value]
  (u/first-error (get all-validations field-name) field-value))

(defn errors [db]
  (let [patient (patient-with-all-fields (get-in db [:ui :new-patient :values]))]
    (->> (for [[field-name field-value] patient
               :let [none? (get-in db [:ui :new-patient :none? field-name])]
               :when (not (true? none?))]
           [field-name (first-error field-name field-value)])
         (into {}))))

(defn scroll-to-end [{:keys [db]} _]
  (.scrollToEnd (get-in db [:ui :new-patient :scroll-view]))
  {})

(defn compute-errors [db _]
  (let [new-errors (errors db)]
    (-> db
        (assoc-in [:ui :new-patient :valid?] (every? nil? (vals new-errors)))
        (assoc-in [:ui :new-patient :errors] new-errors))))

(defn handle-input [{:keys [db]} [_ field-name field-value]]
  (when (#{:gender :village-or-colony} field-name)
    (scroll-to-end {:db db} nil))
  (let [new-db     (assoc-in db [:ui :new-patient :values field-name] field-value)
        new-errors (errors new-db)]
    {:db       new-db
     :dispatch [:compute-errors]}))

(defn register-new-patient [{:keys [db]} _]
  (if (get-in db [:ui :new-patient :valid?])
    (let [patient (new-patient db)]
      {:db (assoc-in db [:store :patients (:id patient)] patient)
       :dispatch-n [[:persist-store]
                    [:set-active-patient-id (:id patient)]
                    [:show-bp-sheet]]})
    {:db (assoc-in db [:ui :new-patient :show-errors?] true)}))

(defn clear [db _]
  (assoc-in db [:ui :new-patient] nil))

(defn set-new-patient-sv-ref [db [_ scroll-view]]
  (assoc-in db [:ui :new-patient :scroll-view] scroll-view))

(defn show-interstitial [db _]
  (assoc-in db [:ui :new-patient :show-interstitial?] true))

(defn hide-interstitial [db _]
  (assoc-in db [:ui :new-patient :show-interstitial?] false))

(defn ui-new-patient-none [{:keys [db]} [_ field-name field-value]]
  {:db (-> db
           (assoc-in [:ui :new-patient :none? field-name] field-value)
           (assoc-in [:ui :new-patient :values field-name] nil))
   :dispatch [:compute-errors]})

(defn register-events []
  (reg-event-fx :scroll-to-end scroll-to-end)
  (reg-event-db :show-interstitial show-interstitial)
  (reg-event-db :hide-interstitial hide-interstitial)
  (reg-event-fx :ui-new-patient handle-input)
  (reg-event-fx :ui-new-patient-none ui-new-patient-none)
  (reg-event-db :new-patient-clear clear)
  (reg-event-db :set-new-patient-sv-ref set-new-patient-sv-ref)
  (reg-event-fx :register-new-patient register-new-patient)
  (reg-event-db :compute-errors compute-errors))
