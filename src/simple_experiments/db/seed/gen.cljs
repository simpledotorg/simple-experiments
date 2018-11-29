(ns simple-experiments.db.seed.gen
  (:require [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [simple-experiments.db.seed.data :as data]
            [simple-experiments.events.utils :as u]
            [simple-experiments.db.seed.spec :as spec]))

(defn time-in-long [days-ago]
  (->> days-ago
       time/days
       (time/minus (time/now))
       timec/to-long))

(defn gen-blood-pressure [blood-pressure]
  (->  blood-pressure
       (assoc
        :created-at
        (time-in-long (:updated-days-ago blood-pressure)))
       (dissoc :updated-days-ago)))

(defn gen-address [state district village-or-colony-index]
  {:village-or-colony (str (if (some? village-or-colony-index)
                             (nth (get-in data/common-addresses [state :village-or-colony])
                                  village-or-colony-index)
                             (rand-nth (get-in data/common-addresses [state :village-or-colony])))
                           ", "
                           (rand-nth (get-in data/common-addresses [state :street-name])))
   :state             state
   :district          district})

(defn gen-prescription-drugs [drugs-ids updated-at]
  (if (empty? drugs-ids)
    {}
    {:protocol-drugs {:drug-ids drugs-ids
                      :updated-at updated-at}}))

(defn assoc-next-visit [{:keys [next-visit-in-days] :as patient}]
  (assoc patient :next-visit
         (timec/to-long
          (time/plus (u/last-visit-time patient)
                     (time/days (or next-visit-in-days 60))))))

(defn card-ids [patient]
  (or (:card-ids patient)
      (set (repeatedly (rand-int 3) random-uuid))))

(defn gen-patient-variants [state district patient]
  (let [blood-pressures    (->> [(:profile patient) :bps]
                                (get-in data/blood-pressure-profiles)
                                (map gen-blood-pressure))
        updated-at         (:created-at (first blood-pressures))
        prescription-drugs (-> data/blood-pressure-profiles
                               (get-in [(:profile patient) :drug-ids])
                               (gen-prescription-drugs updated-at))]
    (-> patient
        (assoc :birth-year (u/birth-year (:age patient)))
        (assoc :id (str (random-uuid)))
        (assoc :blood-pressures blood-pressures)
        (assoc-next-visit)
        (assoc :prescription-drugs prescription-drugs)
        (assoc :card-ids (card-ids patient))
        (merge (gen-address state district (:village-or-colony patient))))))

(defn gen-patients [state district]
  (for [patient-type (:patient-types data/patients)
        :let         [{:keys [variants common]} patient-type]
        variant      variants]
    (gen-patient-variants state district (merge common variant))))

(defn gen-random-patients [num state district]
  (for [patient (gen/sample (s/gen ::spec/patient) num)]
    (gen-patient-variants state district patient)))
