(ns simple-experiments.events.settings
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [simple-experiments.view.components :as c]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.db :as db :refer [app-db]]
            [simple-experiments.events.navigation :as nav]
            [simple-experiments.events.utils :as u :refer [assoc-into-db]]))

(def default-settings
  {:overdue "one-month-later"
   :age-vs-age-or-dob "age"})

(defn change-start-screen [settings]
  (let [start-screen (keyword settings)]
    (nav/reset-screen-stack [start-screen :settings])))

(defn set-setting [{:keys [db]} [_ field value]]
  (if-not (string/blank? value)
    (do (when (= field :start-screen)
          (change-start-screen value))
        {:db (assoc-in db [:store :settings field] value)
         :dispatch [:persist-store]})
    {}))

(defn register-events []
  (reg-event-fx :set-setting set-setting))
