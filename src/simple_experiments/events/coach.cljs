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

(defn set-aadhaar-coach-mark [{:keys [db]} _]
  {:db (if (show-coach-mark? db :aadhaar true)
         (show-coach-mark db :aadhaar)
         (hide-coach-marks db nil))
   :dispatch [:persist-store]})

(defn set-search-coach-mark [{:keys [db]} _]
  {:db (if (show-coach-mark? db :search true)
         (show-coach-mark db :search)
         (hide-coach-marks db nil))
   :dispatch [:persist-store]})

(defn set-scan-coach-mark [{:keys [db]} _]
  {:db (if (show-coach-mark? db :scan true)
         (show-coach-mark db :scan)
         (hide-coach-marks db nil))
   :dispatch [:persist-store]})

(defn set-new-bp-coach-mark [{:keys [db]} _]
  {:db (if (show-coach-mark? db :new-bp true)
         (show-coach-mark db :new-bp)
         (hide-coach-marks db nil))
   :dispatch [:persist-store]})

(defn set-overdue-coach-mark [{:keys [db]} _]
  {:db (if (show-coach-mark? db :overdue true)
         (show-coach-mark db :overdue)
         (hide-coach-marks db nil))
   :dispatch [:persist-store]})

(defn register-events []
  (reg-event-fx :set-search-coach-marks set-search-coach-marks)
  (reg-event-fx :set-scan-coach-mark set-scan-coach-mark)
  (reg-event-fx :set-aadhaar-coach-mark set-aadhaar-coach-mark)
  (reg-event-fx :set-search-coach-mark set-search-coach-mark)
  (reg-event-fx :set-new-bp-coach-mark set-new-bp-coach-mark)
  (reg-event-fx :set-overdue-coach-mark set-overdue-coach-mark)
  (reg-event-db :hide-coach-marks hide-coach-marks)
  (reg-event-fx :set-times-to-show set-times-to-show))
