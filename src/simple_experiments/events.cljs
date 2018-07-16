(ns simple-experiments.events
  (:require
   [re-frame.core :refer [reg-event-db after]]
   [clojure.spec.alpha :as s]
   [simple-experiments.db :as db :refer [app-db]]))

(defn assoc-into-db [k]
  (fn [db [_ & args]]
    (let [ks (butlast args)
          value (last args)]
      (assoc-in db (cons k ks) value))))

(defn set-active-tab [db [_ active-tab]]
  (assoc-in db [:home :active-tab] active-tab))

(defn register-events []
  (reg-event-db :initialize-db (fn [_ _] app-db))
  (reg-event-db :set-active-tab set-active-tab))

(register-events)
