(ns simple-experiments.events.utils
  (:require [clojure.spec.alpha :as s]))

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
