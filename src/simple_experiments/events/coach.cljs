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
      (assoc-in [:ui :coach :multiple-results] false)
      (assoc-in [:ui :coach :aadhaar] false)
      (assoc-in [:ui :coach :home] false)
      (assoc-in [:ui :coach :new-bp] false)))

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

(defn set-times-to-show [db [_ value]]
  (if-not (string/blank? value)
    (assoc-in db [:ui :coach :times-to-show] (js/parseInt value))
    db))

(defn save-times-to-show [{:keys [db]} _]
  {:db (->> (get-in db [:ui :coach :times-to-show])
            (assoc-in db [:store :coach :times-to-show]))
   :dispatch [:persist-store]})

(defn set-aadhaar-coach-mark [{:keys [db]} _]
  {:db (if (show-coach-mark? db :aadhaar true)
         (show-coach-mark db :aadhaar)
         (hide-coach-marks db nil))
   :dispatch [:persist-store]})

(defn set-home-coach-mark [{:keys [db]} _]
  {:db (if (show-coach-mark? db :home true)
         (show-coach-mark db :home)
         (hide-coach-marks db nil))
   :dispatch [:persist-store]})

(defn set-new-bp-coach-mark [{:keys [db]} _]
  {:db (if (show-coach-mark? db :new-bp true)
         (show-coach-mark db :new-bp)
         (hide-coach-marks db nil))
   :dispatch [:persist-store]})

(defn register-events []
  (reg-event-fx :set-search-coach-marks set-search-coach-marks)
  (reg-event-fx :set-aadhaar-coach-mark set-aadhaar-coach-mark)
  (reg-event-fx :set-home-coach-mark set-home-coach-mark)
  (reg-event-fx :set-new-bp-coach-mark set-new-bp-coach-mark)
  (reg-event-db :hide-coach-marks hide-coach-marks)
  (reg-event-db :set-times-to-show set-times-to-show)
  (reg-event-fx :save-times-to-show save-times-to-show))
