(ns simple-experiments.subs
  (:require [re-frame.core :refer [reg-sub]]
            [simple-experiments.events.utils :as u]))

(defn get-in-db [k]
  (fn [db [_ & ks]]
    (get-in db (concat k ks))))

(defn patients-based-on-setting [setting patients]
  (case setting
    "empty"            []
    "one-month-later"  patients
    "six-months-later" (map #(assoc % :overdue-days (rand-int 30)) patients)
    []))

(defn overdue-patients [[patients store-settings filter-by] _]
  (let [filter-fn (cond
                    (= filter-by :all)        identity
                    (= filter-by :one-to-ten) #(<= 1 % 10)
                    :else                     identity)
        patients-with-overdue-days (map #(assoc % :overdue-days (u/overdue-days %))
                                        (vals patients))]
    (->> patients-with-overdue-days
         (patients-based-on-setting (:overdue store-settings))
         (filter #(> (:overdue-days %) 0))
         (remove #(some? (:skip-reason %)))
         (filter #(filter-fn (:overdue-days %)))
         (sort-by :overdue-days >))))

(defn register-subs []
  (reg-sub :active-page (get-in-db [:active-page]))
  (reg-sub :home (get-in-db [:home]))
  (reg-sub :patients (get-in-db [:store :patients]))
  (reg-sub :store-coach (get-in-db [:store :coach]))
  (reg-sub :store-settings (get-in-db [:store :settings]))
  (reg-sub :ui-overdue-list (get-in-db [:ui :overdue-list]))
  (reg-sub :overdue-patients
           :<- [:patients]
           :<- [:store-settings]
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
  (reg-sub :ui-coach (get-in-db [:ui :coach]))
  (reg-sub :seed (get-in-db [:seed])))

(register-subs)
