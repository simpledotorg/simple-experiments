(ns simple-experiments.db.seed
  (:require [simple-experiments.db.patient :as db-p]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]))

(def data
  {:state    "Karnataka"
   :district "Bangalore"
   :patient-types
   [{:name     "A few dhruvs"
     :common   {:gender          "male"
                :phone-number    "9844000111"
                :blood-pressures [{:systolic 150 :diastolic 80 :updated-days-ago 7}
                                  {:systolic 170 :diastolic 90 :updated-days-ago 30}
                                  {:systolic 210 :diastolic 100 :updated-days-ago 70}]
                :protocol-drugs  {:updated-days-ago 10
                                  :drug-ids         #{:am-5 :tel-40}}}
     :variants [{:full-name "Dhruv Saxena" :age 25 :phone-number "8493819392"}
                {:full-name "Dhruv Dutta" :age 25 :phone-number "9998874361"}
                {:full-name "Dhruv Saxena" :age 60 :phone-number "99999998888"}]}]})

(def common-addresses
  {"Karnataka"
   {:street-name
    ["1st Cross" "4th Main" "3rd Cross, 4th Block" "7th A Main"]
    :village-or-colony
    ["Indiranagar" "Jayanagar" "Ulsoor" "HSR Layout" "Koramangla"]}

   "Punjab"
   {:street-name
    ["Bhagat singh colony" "Gandhi Basti" "NFL Colony" "Farid Nagari"
     "Bathinda Road" "Bus Stand Rd" "Hirke Road" "Makhewala Jhanduke Road"]
    :village-or-colony
    ["Bathinda" "Bhagwangarh" "Dannewala" "Nandgarh" "Nathana"
     "Bhikhi" "Budhlada" "Hirke" "Jhanduke" "Mansa" "Bareta"
     "Bhaini" "Bagha" "Sadulgarh" "Sardulewala"]}})

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

(defn gen-address [state district]
  {:street-name       (rand-nth (get-in common-addresses [state :street-name]))
   :village-or-colony (rand-nth (get-in common-addresses [state :village-or-colony]))
   :state             state
   :district          district})

(defn gen-prescription-drugs [protocol-drugs]
  (let [drugs-updated-at (time-in-long (:updated-days-ago protocol-drugs))]
    {:protocol-drugs (-> protocol-drugs
                         (assoc :updated-at drugs-updated-at)
                         (dissoc :updated-days-ago))}))

(defn gen-patient-variants [state district common variant]
  (let [new-patient     (merge common variant)
        blood-pressures (map gen-blood-pressure (:blood-pressures new-patient))]
    (-> new-patient
        (assoc :id (str (random-uuid)))
        (assoc :blood-pressures blood-pressures)
        (assoc :prescription-drugs (gen-prescription-drugs (:protocol-drugs new-patient)))
        (merge (gen-address state district)))))

(defn gen-patients []
  (let [{:keys [patient-types state district]} data]
    (for [patient-type (:patient-types data)
          :let         [{:keys [variants common]} patient-type]
          variant      variants]
      (gen-patient-variants state district common variant))))

(defn patients-by-id []
  (let [patients (gen-patients)]
    (zipmap (map :id patients) patients)))
