(ns simple-experiments.db.seed
  (:require [simple-experiments.db.patient :as db-p]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]))

(def blood-pressure-profiles
  {:htn-months     {:bps      [{:systolic 150 :diastolic 80 :updated-days-ago 30}
                               {:systolic 167 :diastolic 90 :updated-days-ago 37}
                               {:systolic 210 :diastolic 100 :updated-days-ago 67}]
                    :drug-ids #{:am-10 :tel-80}}
   :htn-weeks      {:bps      [{:systolic 140 :diastolic 90 :updated-days-ago 30}
                               {:systolic 167 :diastolic 90 :updated-days-ago 37}]
                    :drug-ids #{:am-10}}
   :htn-days       {:bps      [{:systolic 150 :diastolic 90 :updated-days-ago 7}]
                    :drug-ids #{:am-5}}
   :control-months {:bps      [{:systolic 120 :diastolic 80 :updated-days-ago 30}
                               {:systolic 167 :diastolic 90 :updated-days-ago 37}
                               {:systolic 210 :diastolic 100 :updated-days-ago 67}]
                    :drug-ids #{:am-5}}
   :control-weeks  {:bps      [{:systolic 130 :diastolic 80 :updated-days-ago 30}
                               {:systolic 170 :diastolic 100 :updated-days-ago 37}]
                    :drug-ids #{:am-5}}
   :control-days   {:bps      [{:systolic 120 :diastolic 80 :updated-days-ago 30}]
                    :drug-ids #{}}
   :control-follow {:bps      [{:systolic 120 :diastolic 80 :updated-days-ago 30}
                               {:systolic 117 :diastolic 76 :updated-days-ago 60}]
                    :drug-ids #{}}})

(def data
  {:state    "Karnataka"
   :district "Bangalore"
   :patient-types
   [{:name     "Same full name different age (Madhu)"
     :common   {:full-name "Madhu Mehra"
                :gender "female"
                :profile   :htn-days}
     :variants [{:age 23 :phone-number "9998874361"}
                {:age 25 :phone-number "8543829303"}]}

    {:name     "Same first name different last name (Shreyas)"
     :common   {:age     35
                :gender  "male"
                :profile :htn-weeks}
     :variants [{:full-name "Shreyas Malhotra" :phone-number "8543829303"}
                {:full-name "Shreyas Garewal" :phone-number "8737377273"}]}

    {:name     "Same full name, same age, different locations (Ishita)"
     :common   {:full-name "Ishita Puri"
                :age       45
                :gender    "female"
                :profile   :htn-months}
     :variants [{:village-or-colony 1 :phone-number "8543829303"}
                {:village-or-colony 2 :phone-number "7313829303"}]}

    {:name     "Hypertensives (Datta)"
     :common   {:phone-number "9863728393"}
     :variants [{:full-name "Varun Datta" :gender "male" :age 50 :profile :htn-months}
                {:full-name "Divya Datta" :gender "female" :age 34 :profile :htn-weeks}
                {:full-name "Vani Datta" :gender "female" :age 43 :profile :htn-days}]}

    {:name     "Controls (Khanna)"
     :common   {:phone-number "9863728393"}
     :variants [{:full-name "Abhishek Khanna" :gender "male" :age 50 :profile :control-months}
                {:full-name "Amit Khanna" :gender "male" :age 34 :profile :control-weeks}
                {:full-name "Deepak Khanna " :gender "male" :age 43 :profile :control-days}
                {:full-name "Mahesh Khanna " :gender "male" :age 27 :profile :control-follow}]}]})

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

(defn gen-prescription-drugs [drugs-ids updated-at]
  {:protocol-drugs {:drug-ids drugs-ids
                    :updated-at updated-at}})

(defn gen-patient-variants [state district common variant]
  (let [new-patient        (merge common variant)
        blood-pressures    (->> [(:profile new-patient) :bps]
                                (get-in blood-pressure-profiles)
                                (map gen-blood-pressure))
        updated-at         (:created-at (first blood-pressures))
        prescription-drugs (-> blood-pressure-profiles
                               (get-in [(:profile new-patient) :drug-ids])
                               (gen-prescription-drugs updated-at))]
    (-> new-patient
        (assoc :id (str (random-uuid)))
        (assoc :blood-pressures blood-pressures)
        (assoc :prescription-drugs prescription-drugs)
        (merge (gen-address state district)))))

(defn gen-patients [state district]
  (for [patient-type (:patient-types data)
        :let         [{:keys [variants common]} patient-type]
        variant      variants]
    (gen-patient-variants state district common variant)))

(defn patients-by-id
  ([]
   (patients-by-id (:state data) (:district data)))
  ([db]
   (patients-by-id (get-in db [:seed :state])
                   (get-in db [:seed :district])))
  ([state district]
   (let [patients (gen-patients state district)]
     (zipmap (map :id patients) patients))))
