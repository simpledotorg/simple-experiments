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
  (assoc-in db [:ui :registration] {}))

(defn register-events []
  (reg-event-db :set-registration-field set-registration-field)
  (reg-event-db :registration-done registration-done))
