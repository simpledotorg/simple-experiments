(ns simple-experiments.events.stepper
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

(def schedule-steps
  (vec
   (concat (for [i (range 1 8)] [i :day])
           (for [i (range 2 8)] [i :week])
           (for [i (range 2 13)] [i :month]))))

(defn step-display-str [step-value]
  (let [[n unit] (get schedule-steps step-value)]
    (cond
      (= [1 :day] [n unit])
      "Tomorrow"

      :else
      (str n " " (if (= 1 n)
                   (name unit)
                   (str (name unit) "s"))))))

(defn safe-inc [coll i]
  (if (= (inc i) (count coll)) i (inc i)))

(defn safe-dec [coll i]
  (if (< (dec i) 0) 0 (dec i)))

(defn stepper-fn [path default-val]
  (fn [db [_ direction]]
    (let [stepper            (get-in db path)
          current-step-value (get stepper :current-step-value default-val)
          next-fn            (case direction
                               :next (partial safe-inc schedule-steps)
                               :previous (partial safe-dec schedule-steps))
          next-step-value    (next-fn current-step-value)]
      (->> (assoc stepper
                  :current-step-value next-step-value
                  :current-step (step-display-str next-step-value))
           (assoc-in db path)))))

(defn set-schedule [db [_ patient]]
  (let [stepper            (get-in db [:ui :summary :schedule-stepper])
        current-step-value (get stepper :current-step-value 15)]
    (assoc-in db [:store :patients (:id patient) :scheduled-visit]
              current-step-value)))

(defn register-events []
  (reg-event-db :set-schedule set-schedule)
  (reg-event-db :schedule-stepper (stepper-fn [:ui :summary :schedule-stepper] 9))
  (reg-event-db :reschedule-stepper (stepper-fn [:ui :overdue-list :reschedule-stepper] 1)))
