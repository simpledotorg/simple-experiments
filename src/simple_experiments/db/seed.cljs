(ns simple-experiments.db.seed
  (:require [simple-experiments.db.patient :as db-p]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [simple-experiments.events.utils :as u]))

(def blood-pressure-profiles
  {:htn-months     {:bps      [{:systolic 150 :diastolic 80 :updated-days-ago 32}
                               {:systolic 167 :diastolic 90 :updated-days-ago 37}
                               {:systolic 210 :diastolic 100 :updated-days-ago 67}]
                    :drug-ids #{:am-10 :tel-80}}
   :htn-weeks      {:bps      [{:systolic 140 :diastolic 90 :updated-days-ago 33}
                               {:systolic 167 :diastolic 90 :updated-days-ago 37}]
                    :drug-ids #{:am-10}}
   :htn-days       {:bps      [{:systolic 150 :diastolic 90 :updated-days-ago 7}]
                    :drug-ids #{:am-5}}
   :htn-sudden     {:bps      [{:systolic 160 :diastolic 90 :updated-days-ago 12}
                               {:systolic 117 :diastolic 76 :updated-days-ago 42}]
                    :drug-ids #{:am-10}}
   :control-months {:bps      [{:systolic 120 :diastolic 80 :updated-days-ago 35}
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

(defn birth-year [age]
  (- (time/year (time/now)) age))

(def data
  {:state    "Karnataka"
   :district "Bangalore"
   :patient-types
   [{:name     "Same full name different age (Madhu)"
     :common   {:full-name "Madhu Mehra"
                :gender    "female"
                :profile   :htn-days}
     :variants [{:birth-year (birth-year 23) :phone-number "9998874361"}
                {:birth-year (birth-year 25) :phone-number "8543829303"}]}

    {:name     "Same first name different last name (Shreyas)"
     :common   {:birth-year (birth-year 35)
                :gender     "male"
                :profile    :htn-weeks}
     :variants [{:full-name "Shreyas Malhotra" :phone-number "8543829303"}
                {:full-name "Shreyas Garewal" :phone-number "8737377273"}]}

    ;; TODO: add to call list
    ;; overdue by 2 days, follow up in 30 days, not called before
    {:name     "Same full name, similar ages, different locations (Mahalakshmi)"
     :common   {:full-name "Mahalakshmi Puri"
                :gender    "female"
                :profile   :htn-days}
     :variants [{:village-or-colony 1 :phone-number "8543829303"
                 :birth-year        (birth-year 70)}
                {:village-or-colony 2 :phone-number "9972348065"
                 :birth-year        (birth-year 72)
                 :profile           :htn-months}
                {:village-or-colony 3 :phone-number "9838193939"
                 :birth-year        (birth-year 75)}]}

    ;; TODO: add to call list
    ;; overdue by 7 days, follow up in 5 days, called 4 days ago
    {:name     "Same name, same age, same locations, different phones (Neha)"
     :common   {:full-name          "Neha Gupta"
                :gender             "female"
                :birth-year         (birth-year 40)
                :village-or-colony  4}
     :variants [{:phone-number "9321563635"
                 :profile :htn-sudden
                 :next-visit-in-days 5}
                {:phone-number "7891563635"
                 :profile :htn-days}
                {:phone-number "9838193939"
                 :profile :htn-months}]}

    {:name     "Hypertensives (Datta)"
     :common   {:phone-number "9863728393"}
     :variants [{:full-name  "Varun Datta"
                 :gender     "male"
                 :birth-year (birth-year 50)
                 :profile    :htn-months
                 :next-visit-in-days 1}
                {:full-name  "Divya Datta"
                 :gender     "female"
                 :birth-year (birth-year 34)
                 :profile    :htn-weeks}
                {:full-name  "Vani Datta"
                 :gender     "female"
                 :birth-year (birth-year 43)
                 :profile    :htn-days}]}

    {:name     "Controls (Khanna)"
     :common   {:phone-number "9863728393"}
     :variants [{:full-name  "Abhishek Khanna"
                 :gender     "male"
                 :birth-year (birth-year 50)
                 :profile    :control-months}
                {:full-name  "Amit Khanna"
                 :gender     "male"
                 :birth-year (birth-year 34)
                 :profile    :control-weeks}
                {:full-name  "Deepak Khanna "
                 :gender     "male"
                 :birth-year (birth-year 43)
                 :profile    :control-days}
                {:full-name  "Mahesh Khanna "
                 :gender     "male"
                 :birth-year (birth-year 27)
                 :profile    :control-follow}]}

    ;; TODO: add some with overdue > 10 days
    ]})

(def common-addresses
  {"Karnataka"
   {:street-name
    ["1st Cross" "4th Main" "3rd Cross, 4th Block" "7th A Main"]
    :village-or-colony
    ["Indiranagar" "Jayanagar" "Ulsoor" "HSR Layout" "RT Nagar"]}

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

(defn gen-address [state district village-or-colony-index]
  {:village-or-colony (str (if (some? village-or-colony-index)
                             (nth (get-in common-addresses [state :village-or-colony])
                                  village-or-colony-index)
                             (rand-nth (get-in common-addresses [state :village-or-colony])))
                           ", "
                           (rand-nth (get-in common-addresses [state :street-name])))
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
                     (time/days (or next-visit-in-days 30))))))

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
        (assoc-next-visit)
        (assoc :prescription-drugs prescription-drugs)
        (merge (gen-address state district (:village-or-colony new-patient))))))

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
