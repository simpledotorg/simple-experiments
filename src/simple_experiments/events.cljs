(ns simple-experiments.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [re-frame-fx.dispatch]
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
  {:db (update-in db [:store :patients] conj patient)
   :dispatch [:persist-store]})

(defn goto [db [_ page]]
  (assoc db :active-page page))

(defn search-patients [db [_ search-query]]
  (let [pattern (re-pattern (string/trim search-query))]
    (->> (get-in db [:store :patients])
         (filter #(re-find pattern (:full-name %)))
         (assoc db :patient-search-results))))

(defn handle-search-patients [_ [_ search-query]]
  {:dispatch-debounce
   [{:id ::search-patients-on-input-change
     :timeout 250
     :action :dispatch
     :event [:search-patients search-query]}]})

(defn set-active-patient [{:keys [db]} [_ patient]]
  {:db (assoc db :active-patient patient)
   :dispatch [:goto :patient-summary]})

(defn register-events []
  (reg-event-db :initialize-db (fn [_ _] app-db))
  (reg-event-db :set-active-tab set-active-tab)
  (reg-event-fx :add-patient add-patient)
  (reg-event-db :goto goto)
  (reg-event-db :search-patients search-patients)
  (reg-event-fx :handle-search-patients handle-search-patients)
  (reg-event-fx :set-active-patient set-active-patient))

(register-events)
