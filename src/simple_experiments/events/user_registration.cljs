(ns simple-experiments.events.user-registration
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

(defn set-registration-field [db [_ field-name field-value]]
  (assoc-in db [:ui :registration field-name] field-value))

(defn registration-done [db _]
  (-> db
      (assoc-in [:ui :registration] {})
      (assoc-in [:store :settings :approval-status] "requested")))

(defn verify-security-pin [{:keys [db]} _]
  (let [pin              (get-in db [:ui :registration :security-pin])
        pin-confirmation (get-in db [:ui :registration :security-pin-verification])]
    (if (= pin pin-confirmation)
      {:dispatch [:goto :location-access]}
      {:db (assoc-in db [:ui :registration :pin-mismatch?] true)})))

(defn register-events []
  (reg-event-fx :verify-security-pin verify-security-pin)
  (reg-event-db :set-registration-field set-registration-field)
  (reg-event-db :registration-done registration-done))
