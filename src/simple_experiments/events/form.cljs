(ns simple-experiments.events.form
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
  {:full-name         [{:spec ::db-p/non-empty-string :error "Please enter patient's full name."}]
   :age               [{:spec ::db-p/non-empty-string :error "Please enter patient's age"}
                       {:spec ::db-p/age-string :error "Please enter a valid age"}]
   :date-of-birth     [{:spec ::db-p/non-empty-string :error "Please enter patient's date of birth"}
                       {:spec ::db-p/date-of-birth-string :error "Please enter a valid date of birth"}]
   :gender            [{:spec ::db-p/non-empty-string :error "Please select a gender."}
                       {:spec ::db-p/gender :error "Please enter a valid gender."}]
   :phone-number      [{:spec ::db-p/non-empty-string :error "Please enter a phone number or select 'No phone'."}
                       {:spec ::db-p/phone-number :error "Please enter a valid phone number."}]
   :village-or-colony [{:spec ::db-p/non-empty-string :error "Please enter a village or colony or select 'No colony'."}]})

(defn patient-with-all-fields [patient active-input]
  (let [alternate-field (first (disj #{:age :date-of-birth} active-input))]
    (dissoc (->> db-p/patient-fields
                 (map (fn [f] [f (patient f)]))
                 (into {}))
            alternate-field)))

(defn first-error [field-name field-value]
  (u/first-error (get all-validations field-name) field-value))

(defn errors [db]
  (let [active-input (get-in db [:ui :patient-form :active-input])
        patient (patient-with-all-fields (get-in db [:ui :patient-form :values]) active-input)]
    (->> (for [[field-name field-value] patient
               :let [none? (get-in db [:ui :patient-form :none? field-name])]
               :when (not (true? none?))]
           [field-name (first-error field-name field-value)])
         (into {}))))

(defn scroll-to-end [{:keys [db]} _]
  (when (some? (get-in db [:ui :patient-form :scroll-view]))
    (.scrollToEnd (get-in db [:ui :patient-form :scroll-view])))
  {})

(defn compute-errors [db _]
  (let [new-errors (errors db)]
    (-> db
        (assoc-in [:ui :patient-form :valid?] (every? nil? (vals new-errors)))
        (assoc-in [:ui :patient-form :errors] new-errors))))

(defn augment-dob-string [dob-string]
  (if (or (= (count dob-string) 2)
          (= (count dob-string) 5))
    (str dob-string "/")
    dob-string))

(defn active-input [fields]
  (cond
    (not (string/blank? (:date-of-birth fields)))
    :date-of-birth

    (not (string/blank? (:age fields)))
    :age

    :else
    :none))

(defn handle-input [{:keys [db]} [_ field-name field-value]]
  (when (#{:gender :village-or-colony} field-name)
    (scroll-to-end {:db db} nil))
  (let [v          (if (= field-name :date-of-birth)
                     (augment-dob-string field-value)
                     field-value)
        new-db     (assoc-in db [:ui :patient-form :values field-name] v)
        new-errors (errors new-db)
        ai         (active-input (get-in new-db [:ui :patient-form :values]))]
    {:db       (assoc-in new-db [:ui :patient-form :active-input] ai)
     :dispatch [:compute-patient-form-errors]}))

(defn clear [db _]
  (assoc-in db [:ui :patient-form] nil))

(defn set-patient-form-sv-ref [db [_ scroll-view]]
  (assoc-in db [:ui :patient-form :scroll-view] scroll-view))

(defn ui-patient-form-none [{:keys [db]} [_ field-name field-value]]
  {:db (-> db
           (assoc-in [:ui :patient-form :none? field-name] field-value)
           (assoc-in [:ui :patient-form :values field-name] nil))
   :dispatch [:compute-patient-form-errors]})

(defn patient-from-form [form-data]
  (let [patient      (:values form-data)
        active-input (:active-input form-data)
        dob-string   (:date-of-birth patient)
        dob          (when (not (string/blank? dob-string))
                       (timec/to-long (u/dob-string->time dob-string)))
        age          (if (some? dob)
                       (u/dob-string->age dob-string)
                       (:age patient))]
    (-> patient
        (patient-with-all-fields active-input)
        (merge {:id            (or (:id form-data) (str (random-uuid)))
                :date-of-birth dob
                :age           age}))))

(defn register-events []
  (reg-event-fx :patient-form-scroll-to-end scroll-to-end)
  (reg-event-fx :ui-patient-form handle-input)
  (reg-event-fx :ui-patient-form-none ui-patient-form-none)
  (reg-event-db :patient-form-clear clear)
  (reg-event-db :set-patient-form-sv-ref set-patient-form-sv-ref)
  (reg-event-db :compute-patient-form-errors compute-errors))
