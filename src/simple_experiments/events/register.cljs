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
            [simple-experiments.events.utils :refer [assoc-into-db]]))

(def patient-fields
  #{:id :full-name :age :phone-number :gender
    :village-or-colony :district :state})

(defn new-patient [db]
  (into {:id (str (random-uuid))}
        (map (fn [[field-name {:keys [value]}]]
               [field-name value])
             (get-in db [:ui :new-patient :fields]))))

(defn valid? [db]
  (every? #(not (string/blank? %))
          (map (new-patient db) patient-fields)))

(defn handle-input [db [_ path field-name field-value]]
  (when (= :gender field-name)
    (.scrollToEnd (get-in db [:ui :new-patient :scroll-view])))
  (let [x (assoc-in db [:ui :new-patient path field-name :value] field-value)]
    (assoc-in x [:ui :new-patient :valid?] (valid? x))))

(defn register-new-patient [{:keys [db]} _]
  (let [patient (new-patient db)]
    {:db (assoc-in db [:store :patients (:id patient)] patient)
     :dispatch-n [[:goto :patient-summary]
                  [:set-active-patient-id (:id patient)]]}))

(defn clear [db _]
  (assoc-in db [:ui :new-patient] nil))

(defn set-new-patient-sv-ref [db [_ scroll-view]]
  (assoc-in db [:ui :new-patient :scroll-view] scroll-view))

(defn register-events []
  (reg-event-db :ui-new-patient handle-input)
  (reg-event-db :new-patient-clear clear)
  (reg-event-db :set-new-patient-sv-ref set-new-patient-sv-ref)
  (reg-event-fx :register-new-patient register-new-patient))
