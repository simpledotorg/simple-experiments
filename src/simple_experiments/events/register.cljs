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

(defn new-patient [db]
  (let [patient      (get-in db [:ui :new-patient :values])
        active-input (get-in db [:ui :new-patient :active-input])
        dob-string   (:date-of-birth patient)
        dob          (when (not (string/blank? dob-string))
                       (timec/to-long (u/dob-string->time dob-string)))
        age          (if (some? dob)
                       (u/dob-string->age dob-string)
                       (:age patient))]
    (-> patient
        (patient-with-all-fields active-input)
        (merge {:id            (str (random-uuid))
                :date-of-birth dob
                :age           age}))))

(defn first-error [field-name field-value]
  (u/first-error (get all-validations field-name) field-value))

(defn errors [db]
  (let [active-input (get-in db [:ui :new-patient :active-input])
        patient (patient-with-all-fields (get-in db [:ui :new-patient :values]) active-input)]
    (->> (for [[field-name field-value] patient
               :let [none? (get-in db [:ui :new-patient :none? field-name])]
               :when (not (true? none?))]
           [field-name (first-error field-name field-value)])
         (into {}))))

(defn scroll-to-end [{:keys [db]} _]
  (when (some? (get-in db [:ui :new-patient :scroll-view]))
    (.scrollToEnd (get-in db [:ui :new-patient :scroll-view])))
  {})

(defn compute-errors [db _]
  (let [new-errors (errors db)]
    (-> db
        (assoc-in [:ui :new-patient :valid?] (every? nil? (vals new-errors)))
        (assoc-in [:ui :new-patient :errors] new-errors))))

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
        new-db     (assoc-in db [:ui :new-patient :values field-name] v)
        new-errors (errors new-db)
        ai         (active-input (get-in new-db [:ui :new-patient :values]))]
    {:db       (assoc-in new-db [:ui :new-patient :active-input] ai)
     :dispatch [:compute-errors]}))

(defn register-new-patient [{:keys [db]} _]
  (if (get-in db [:ui :new-patient :valid?])
    (let [active-card (-> db :ui :active-card)
          patient (let [patient (new-patient db)]
                    (if active-card
                      (assoc patient :card-uuids #{(:uuid active-card)})
                      patient))
          registration-complete-events [[:persist-store]
                                        [:set-active-patient-id (:id patient)]
                                        [:show-bp-sheet]]]
      {:db (assoc-in db [:store :patients (:id patient)] patient)
       :dispatch-n registration-complete-events})
    {:db (assoc-in db [:ui :new-patient :show-errors?] true)}))

(defn clear [db _]
  (assoc-in db [:ui :new-patient] nil))

(defn set-new-patient-sv-ref [db [_ scroll-view]]
  (assoc-in db [:ui :new-patient :scroll-view] scroll-view))

(defn show-schedule-sheet [{:keys [db]} _]
  {:db (assoc-in db [:ui :summary :show-schedule-sheet?] true)
   :dispatch [:schedule-next-visit 30]})

(defn hide-schedule-sheet [db _]
  (assoc-in db [:ui :summary :show-schedule-sheet?] false))

(defn ui-new-patient-none [{:keys [db]} [_ field-name field-value]]
  {:db (-> db
           (assoc-in [:ui :new-patient :none? field-name] field-value)
           (assoc-in [:ui :new-patient :values field-name] nil))
   :dispatch [:compute-errors]})

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

(defn register-events []
  (reg-event-fx :scroll-to-end scroll-to-end)
  (reg-event-fx :show-schedule-sheet show-schedule-sheet)
  (reg-event-db :hide-schedule-sheet hide-schedule-sheet)
  (reg-event-fx :ui-new-patient handle-input)
  (reg-event-fx :ui-new-patient-none ui-new-patient-none)
  (reg-event-db :new-patient-clear clear)
  (reg-event-db :set-new-patient-sv-ref set-new-patient-sv-ref)
  (reg-event-fx :register-new-patient register-new-patient)
  (reg-event-fx :schedule-next-visit schedule-next-visit)
  (reg-event-db :compute-errors compute-errors)
  (reg-event-fx :summary-save summary-save))
