(ns simple-experiments.events.utils
  (:require [clojure.spec.alpha :as s]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]))

(defn assoc-into-db [iks]
  (fn [db [_ & args]]
    (let [ks (butlast args)
          value (last args)]
      (assoc-in db (concat iks ks) value))))

(defn first-error [validations field-value]
  (some
   (fn [{:keys [spec error]}]
     (if (s/valid? spec field-value)
       nil
       error))
   validations))

(defn age [birth-year]
  (let [birth-year-int (if (string? birth-year)
                         (js/parseInt birth-year)
                         birth-year)]
    (time/in-years (time/interval (time/date-time birth-year-int) (time/now)))))

(defn last-visit-time [{:keys [blood-pressures] :as patient}]
  (-> (apply max (map :created-at blood-pressures))
      timec/from-long))

(defn last-visit [{:keys [blood-pressures] :as patient}]
  (-> patient
      last-visit-time
      (time/interval (time/now))
      time/in-days))

(defn days-ago-text [days-ago]
  (cond (= 0 days-ago)
        "Today"

        (= 1 days-ago)
        "Yesterday"

        :else
        (str days-ago " days ago")))

(defn overdue-days [{:keys [next-visit] :as patient}]
  (when (some? next-visit)
    (let [next-visit (timec/from-long next-visit)]
      (if (time/after? next-visit (time/now))
        nil
        (time/in-days (time/interval next-visit (time/now)))))))

(defn latest-bp [{:keys [blood-pressures] :as patient}]
  (first (sort-by :created-at > blood-pressures)))

(defn active-patient-id [db]
  (get-in db [:ui :active-patient-id]))
