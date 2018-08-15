(ns simple-experiments.events.coach
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

(defn hide-coach-marks [db _]
  (-> db
      (assoc-in [:ui :coach :single-result] false)
      (assoc-in [:ui :coach :multiple-results] false)))

(defn show-coach-mark [db coach-type]
  (-> db
      (assoc-in [:ui :coach coach-type] true)
      (update-in [:store :coach :times-shown coach-type] inc)))

(defn show-coach-mark? [db coach-type show?]
  (let [times-to-show (or (get-in db [:store :coach :times-to-show]) 1)
        times-shown (get-in db [:store :coach :times-shown coach-type])]
    (and show? (< times-shown times-to-show))))

(defn set-search-coach-marks [{:keys [db]} _]
  (let [results (get-in db [:ui :coach :results])
        times-to-show (or (get-in db [:store :coach :times-to-show]) 1)
        times-shown (get-in db [:store :coach :times-shown])]
    {:db
     (cond
       (show-coach-mark? db :single-result
                         (= 1 (count results)))
       (show-coach-mark db :single-result)

       (show-coach-mark? db :multiple-results
                         (apply = (map :full-name results)))
       (show-coach-mark db :multiple-results)

       :else
       (hide-coach-marks db nil))
     :dispatch [:persist-store]}))

(defn register-events []
  (reg-event-fx :set-search-coach-marks set-search-coach-marks)
  (reg-event-db :hide-coach-marks hide-coach-marks))
