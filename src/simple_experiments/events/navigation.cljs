(ns simple-experiments.events.navigation
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [simple-experiments.view.components :as c]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.db :as db :refer [app-db]]))

(defonce screen-stack
  (atom [:home]))

(def special-cases
  {[:home :patient-list :patient-summary]              [:home]
   [:home :patient-list :new-patient :patient-summary] [:home]})

(defn go-back [db _]
  (cond
    (= @screen-stack [:home])
    (do (.exitApp c/back-handler)
        db)

    (special-cases @screen-stack)
    (do
      (swap! screen-stack special-cases)
      (assoc db :active-page (last @screen-stack)))

    :else
    (do
      (swap! screen-stack pop)
      (assoc db :active-page (last @screen-stack)))))

(defn goto [db [_ page]]
  (swap! screen-stack conj page)
  (assoc db :active-page (last @screen-stack)))

(defn register-events []
  (reg-event-db :goto goto)
  (reg-event-db :go-back go-back))
