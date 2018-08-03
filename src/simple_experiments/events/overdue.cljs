(ns simple-experiments.events.overdue
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

(defn set-overdue-filter [db [_ filter-by]]
  (assoc-in db [:ui :overdue-list :filter-by] filter-by))

(defn register-events []
  (reg-event-db :set-overdue-filter set-overdue-filter))
