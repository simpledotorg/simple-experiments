(ns simple-experiments.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [simple-experiments.db :as db :refer [app-db]]))

(defn assoc-into-db [k]
  (fn [db [_ & args]]
    (let [ks (butlast args)
          value (last args)]
      (assoc-in db (cons k ks) value))))

(defn set-active-tab [db [_ active-tab]]
  (assoc-in db [:home :active-tab] active-tab))

(defn add-patient [{:keys [db]} [_ patient]]
  {:db (assoc-in db [:store :patients (:id patient)] patient)
   :dispatch [:persist-store]})

(defn goto [db [_ page]]
  (assoc db :active-page page))

(defn search-patients [db [_ search-query]]
  (let [pattern (re-pattern (string/trim search-query))]
    (->> (get-in db [:store :patients])
         vals
         (filter #(re-find pattern (:full-name %)))
         (assoc db :patient-search-results))))

(defn handle-search-patients [_ [_ search-query]]
  {:dispatch-debounce
   [{:id ::search-patients-on-input-change
     :timeout 250
     :action :dispatch
     :event [:search-patients search-query]}]})

(defn set-active-patient-id [{:keys [db]} [_ patient-id]]
  {:db (assoc-in db [:ui :active-patient-id] patient-id)
   :dispatch [:goto :patient-summary]})

(defn handle-bp-keyboard [{:keys [db]} [_ kind value]]
  (cond
    (and (= kind :systolic)
         (or (and (re-find #"^[12]" value)
                  (= 3 (count value)))
             (and (not (re-find #"^[12]" value))
                  (= 2 (count value)))))
    (.focus (get-in db [:ui :bp :diastolic :ref])))
  {:db (assoc-in db [:ui :bp :value kind] value)})

(defn show-bp-sheet [db _]
  (assoc-in db [:ui :bp :visible?] true))

(defn hide-bp-sheet [db _]
  (assoc-in db [:ui :bp :visible?] false))

(defn set-bp-ref [db [_ kind ref]]
  (assoc-in db [:ui :bp kind :ref] ref))

(defn fetch-new-bp [db]
  (let [ts (timec/to-long (time/now))]
    {:systolic (get-in db [:ui :bp :value :systolic])
     :diastolic (get-in db [:ui :bp :value :diastolic])
     :created-at ts
     :updated-at ts}))

(defn save-bp [{:keys [db]} _]
  (let [active-patient-id (get-in db [:ui :active-patient-id])]
    {:db (update-in
          db
          [:store :patients active-patient-id :blood-pressures]
          conj
          (fetch-new-bp db))
     :dispatch [:hide-bp-sheet]}))

(defn register-events []
  (reg-event-db :initialize-db (fn [_ _] app-db))
  (reg-event-db :set-active-tab set-active-tab)
  (reg-event-fx :add-patient add-patient)
  (reg-event-db :goto goto)
  (reg-event-db :search-patients search-patients)
  (reg-event-fx :handle-search-patients handle-search-patients)
  (reg-event-fx :set-active-patient-id set-active-patient-id)
  (reg-event-db :show-bp-sheet show-bp-sheet)
  (reg-event-db :hide-bp-sheet hide-bp-sheet)
  (reg-event-fx :handle-bp-keyboard handle-bp-keyboard)
  (reg-event-db :set-bp-ref set-bp-ref)
  (reg-event-fx :save-bp save-bp))

(register-events)
