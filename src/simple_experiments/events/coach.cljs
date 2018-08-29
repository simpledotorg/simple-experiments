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
  (assoc-in db [:ui :coach] {}))

(defn show-coach-mark [db coach-type]
  (-> db
      (assoc-in [:ui :coach coach-type] true)
      (update-in [:store :coach :times-shown coach-type] inc)))

(defn show-coach-mark? [db coach-type show?]
  (let [times-to-show (or (get-in db [:store :coach :times-to-show]) 1)
        times-shown (get-in db [:store :coach :times-shown coach-type])]
    (and show? (< times-shown times-to-show))))

(defn set-search-coach-marks [{:keys [db]} _]
  (let [results (get-in db [:ui :patient-search :results])
        times-to-show (or (get-in db [:store :coach :times-to-show]) 1)
        times-shown (get-in db [:store :coach :times-shown])]
    {:db
     (cond
       (show-coach-mark? db :single-result
                         (= 1 (count results)))
       (show-coach-mark db :single-result)

       (show-coach-mark? db :multiple-results
                         (and (apply = (map :full-name results))
                              (>= (count results) 2)))
       (show-coach-mark db :multiple-results)

       :else
       (hide-coach-marks db nil))
     :dispatch [:persist-store]}))

(defn set-times-to-show [{:keys [db]} [_ value]]
  (if-not (string/blank? value)
    {:db (assoc-in db [:store :coach :times-to-show] (js/parseInt value))
     :dispatch [:persist-store]}
    {}))

(defn set-coach-mark [{:keys [db]} [_ coach-name]]
  {:db (if (show-coach-mark? db coach-name true)
         (show-coach-mark db coach-name)
         (hide-coach-marks db nil))
   :dispatch [:persist-store]})

(defn set-aadhaar-coach-mark [cofx _]
  (set-coach-mark cofx [nil :aadhaar]))

(defn set-search-coach-mark [cofx _]
  (set-coach-mark cofx [nil :search]))

(defn set-scan-coach-mark [cofx _]
  (set-coach-mark cofx [nil :scan]))

(defn set-new-bp-coach-mark [cofx _]
  (set-coach-mark cofx [nil :new-bp]))

(defn set-overdue-coach-mark [cofx _]
  (set-coach-mark cofx [nil :overdue]))

(defn set-call-coach-mark [cofx _]
  (set-coach-mark cofx [nil :call]))

(defn set-patient-status-coach-mark [cofx _]
  (set-coach-mark cofx [nil :patient-status]))

(defn register-events []
  (reg-event-fx :set-search-coach-marks set-search-coach-marks)
  (reg-event-fx :set-scan-coach-mark set-scan-coach-mark)
  (reg-event-fx :set-aadhaar-coach-mark set-aadhaar-coach-mark)
  (reg-event-fx :set-search-coach-mark set-search-coach-mark)
  (reg-event-fx :set-new-bp-coach-mark set-new-bp-coach-mark)
  (reg-event-fx :set-overdue-coach-mark set-overdue-coach-mark)
  (reg-event-fx :set-call-coach-mark set-call-coach-mark)
  (reg-event-fx :set-patient-status-coach-mark set-patient-status-coach-mark)
  (reg-event-db :hide-coach-marks hide-coach-marks)
  (reg-event-fx :set-times-to-show set-times-to-show))
