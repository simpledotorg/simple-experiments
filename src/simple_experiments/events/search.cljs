(ns simple-experiments.events.search
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [simple-experiments.view.components :as c]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.db :as db :refer [app-db]]
            [simple-experiments.events.utils :as u :refer [assoc-into-db]]))

(def all-validations
  {:full-name     [{:spec ::db-p/non-empty-string :error "Please enter patient's full name."}]
   :age           [{:spec ::db-p/non-empty-string :error "Please enter patient's age."}
                   {:spec ::db-p/age-string :error "Please enter a valid age."}]
   :date-of-birth [{:spec ::db-p/non-empty-string :error "Please enter patient's date of birth."}
                   {:spec ::db-p/date-of-birth-string :error "Please enter a valid date of birth"}]})

(defn errors [db]
  (let [setting       (get-in db [:store :settings :age-vs-age-or-dob])
        full-name     (get-in db [:ui :patient-search :full-name])
        age           (get-in db [:ui :patient-search :age])
        date-of-birth (get-in db [:ui :patient-search :date-of-birth])]
    {:full-name     (u/first-error (:full-name all-validations) full-name)
     :age           (if (or (= setting "age")
                            (and (= setting "age-or-dob")
                                 (string/blank? date-of-birth)))
                      (u/first-error (:age all-validations) age))
     :date-of-birth (if (and (= setting "age-or-dob")
                             (string/blank? age))
                      (u/first-error (:date-of-birth all-validations) date-of-birth))}))

(defn enable-next? [db]
  (every? nil? (vals (errors db))))

(defn search-patients [{:keys [db]} _]
  (if (enable-next? db)
    (let [full-name (get-in db [:ui :patient-search :full-name])
          setting (get-in db [:store :settings :age-vs-age-or-dob])
          age-string (get-in db [:ui :patient-search :age])
          dob-string (get-in db [:ui :patient-search :date-of-birth])
          age (if (not (string/blank? age-string))
                (js/parseInt age-string)
                (u/dob-string->age dob-string))
          pattern (re-pattern (str "(?i)" (string/trim full-name)))]
      {:db (->> (get-in db [:store :patients])
                vals
                (filter #(<= (- age 5) (:age %) (+ 5 age)))
                (filter #(re-find pattern (:full-name %)))
                (assoc-in db [:ui :patient-search :results]))
       :dispatch-n [[:goto :patient-list]
                    [:goto-select-mode]
                    [:set-search-coach-marks]]})
    {:db (-> db
             (assoc-in [:ui :patient-search :show-errors?] true)
             (assoc-in [:ui :patient-search :errors] (errors db)))}))

(defn augment-dob-string [dob-string]
  (if (or (= (count dob-string) 2)
          (= (count dob-string) 5))
    (str dob-string "/")
    dob-string))

(defn handle-patient-search [db [_ k v]]
  (let [v (if (= k :date-of-birth) (augment-dob-string v) v)
        new-db (assoc-in db [:ui :patient-search k] v)
        new-errors (errors new-db)]
    (-> new-db
        (assoc-in [:ui :patient-search :enable-next?] (enable-next? new-db))
        (assoc-in [:ui :patient-search :errors] new-errors))))

(defn goto-select-mode [db _]
  (.dismiss c/keyboard)
  (assoc-in db [:ui :patient-search :mode] :select))

(defn goto-search-mode [db _]
  (-> db
      (assoc-in [:ui :patient-search :mode] :search)
      (assoc-in [:ui :patient-search :enable-next?] (enable-next? db))
      (assoc-in [:ui :patient-search :results] nil)))

(defn clear [db _]
  (assoc-in db [:ui :patient-search] nil))

(defn register-events []
  (reg-event-db :ui-patient-search handle-patient-search)
  (reg-event-db :patient-search-clear clear)
  (reg-event-fx :search-patients search-patients)
  (reg-event-db :goto-select-mode goto-select-mode)
  (reg-event-db :goto-search-mode goto-search-mode))
