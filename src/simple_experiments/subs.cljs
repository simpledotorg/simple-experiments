(ns simple-experiments.subs
  (:require [re-frame.core :refer [reg-sub]]
            [simple-experiments.events.utils :as u]))

(defn get-in-db [k]
  (fn [db [_ & ks]]
    (get-in db (concat k ks))))

(defn overdue-patients [[patients filter-by] _]
  (let [filter-fn (cond
                    (= filter-by :all)        identity
                    (= filter-by :one-to-ten) #(<= 1 % 10)
                    :else                     identity)]
    (->> patients
         vals
         (map #(assoc % :overdue-days (u/overdue-days %)))
         (filter #(> (:overdue-days %) 0))
         (filter #(filter-fn (:overdue-days %)))
         (sort-by :overdue-days >))))

(defn register-subs []
  (reg-sub :active-page (get-in-db [:active-page]))
  (reg-sub :home (get-in-db [:home]))
  (reg-sub :patients (get-in-db [:store :patients]))
  (reg-sub :ui-overdue-list (get-in-db [:ui :overdue-list]))
  (reg-sub :overdue-patients
           :<- [:patients]
           :<- [:ui-overdue-list :filter-by] overdue-patients)
  (reg-sub :patient-search-results (get-in-db [:patient-search-results]))
  (reg-sub :active-patient-id (get-in-db [:ui :active-patient-id]))
  (reg-sub :ui-bp (get-in-db [:ui :bp]))
  (reg-sub :ui-custom-drug (get-in-db [:ui :custom-drug]))
  (reg-sub :ui-bp-focus (get-in-db [:ui :bp :focus]))
  (reg-sub :ui-text-input-layout (get-in-db [:ui :text-input-layout]))
  (reg-sub :ui-patient-search (get-in-db [:ui :patient-search]))
  (reg-sub :ui-new-patient (get-in-db [:ui :new-patient]))
  (reg-sub :ui-summary (get-in-db [:ui :summary]))
  (reg-sub :seed (get-in-db [:seed])))

(register-subs)
