(ns simple-experiments.db.seed
  (:require [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [simple-experiments.db.seed.data :as data]
            [simple-experiments.db.seed.gen :as gen]))

(defn patients-by-id
  ([]
   (patients-by-id (:state data/patients)
                   (:district data/patients)))
  ([db]
   (patients-by-id (get-in db [:seed :state])
                   (get-in db [:seed :district])))
  ([state district]
   (let [patients (map #(assoc % :type :seed)
                       (gen/gen-patients state district))
         random-patients (map #(assoc % :type :random)
                              (gen/gen-random-patients 20 state district))
         all-patients (concat patients random-patients)]
     (zipmap (map :id all-patients) all-patients))))
