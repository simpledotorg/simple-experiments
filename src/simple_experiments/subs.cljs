(ns simple-experiments.subs
  (:require [re-frame.core :refer [reg-sub]]))

(defn get-in-db [k]
  (fn [db [_ & ks]]
    (get-in db (cons k ks))))

(defn register-subs []
  (reg-sub :home (get-in-db :home)))

(register-subs)
