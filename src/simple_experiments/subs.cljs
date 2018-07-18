(ns simple-experiments.subs
  (:require [re-frame.core :refer [reg-sub]]))

(defn get-in-db [k]
  (fn [db [_ & ks]]
    (get-in db (concat k ks))))

(defn register-subs []
  (reg-sub :active-page (get-in-db [:active-page]))
  (reg-sub :home (get-in-db [:home]))
  (reg-sub :patients (get-in-db [:store :patients]))
  (reg-sub :patient-search-results (get-in-db [:patient-search-results]))
  (reg-sub :active-patient-id (get-in-db [:ui :active-patient-id]))
  (reg-sub :ui-bp (get-in-db [:ui :bp]))
  (reg-sub :ui-bp-focus (get-in-db [:ui :bp :focus])))

(register-subs)
