(ns simple-experiments.db.seed.data
  (:require [cljs-time.core :as time]
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
   :htn-sudden     {:bps      [{:systolic 160 :diastolic 90 :updated-days-ago 17}
                               {:systolic 117 :diastolic 76 :updated-days-ago 47}]
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

(def patients
  {:state    "Karnataka"
   :district "Bangalore"
   :patient-types
   [{:name     "Same full name different age (Madhu)"
     :common   {:full-name "Madhu Mehra"
                :gender    "female"
                :profile   :htn-days}
     :variants [{:age 23 :phone-number "9998874361"}
                {:age 25 :phone-number "8543829303"}]}

    {:name     "Same first name different last name (Shreyas)"
     :common   {:age     35
                :gender  "male"
                :profile :htn-weeks}
     :variants [{:full-name "Shreyas Malhotra" :phone-number "8543829303"}
                {:full-name "Shreyas Garewal" :phone-number "8737377273"}]}

    {:name     "Same full name, similar ages, different locations (Mahalakshmi)"
     :common   {:full-name "Mahalakshmi Puri"
                :gender    "female"
                :profile   :htn-days}
     :variants [{:village-or-colony 1 :phone-number "8543829303"
                 :age               70}
                {:village-or-colony  2 :phone-number "9972348065"
                 :age                72
                 :profile            :htn-months
                 :next-visit-in-days 15
                 :called-at          (u/days-ago 7)}
                {:village-or-colony 3 :phone-number "9838193939"
                 :age               75}]}

    {:name     "Same name, same age, same locations, different phones (Neha)"
     :common   {:full-name         "Neha Gupta"
                :gender            "female"
                :age               40
                :village-or-colony 4}
     :variants [{:phone-number       "9321563635"
                 :profile            :htn-sudden
                 :next-visit-in-days 5}
                {:phone-number "7891563635"
                 :profile      :htn-days}
                {:phone-number "9838193939"
                 :profile      :htn-months}]}

    {:name     "Hypertensives (Datta)"
     :common   {:phone-number "9863728393"}
     :variants [{:full-name          "Varun Datta"
                 :gender             "male"
                 :age                50
                 :profile            :htn-months
                 :next-visit-in-days -26
                 :called-at          (u/days-ago 5)}
                {:full-name          "Divya Datta"
                 :gender             "female"
                 :age                34
                 :profile            :htn-weeks
                 :next-visit-in-days 9
                 :called-at          (u/days-ago 2)}
                {:full-name          "Vani Datta"
                 :gender             "female"
                 :age                43
                 :profile            :htn-days
                 :next-visit-in-days 6}]}

    {:name     "Controls (Khanna)"
     :common   {:phone-number "9863728393"}
     :variants [{:full-name "Abhishek Khanna"
                 :gender    "male"
                 :age       50
                 :profile   :control-months}
                {:full-name "Amit Khanna"
                 :gender    "male"
                 :age       34
                 :profile   :control-weeks}
                {:full-name "Deepak Khanna "
                 :gender    "male"
                 :age       43
                 :profile   :control-days}
                {:full-name "Mahesh Khanna "
                 :gender    "male"
                 :age       27
                 :profile   :control-follow}]}]})

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

(def common-names
  {:female      #{"Anjali" "Divya" "Ishita" "Priya" "Priyanka"
                  "Riya" "Shreya" "Tanvi" "Tanya" "Vani"},
   :male        #{"Abhishek" "Adityaamit" "Ankit" "Deepak" "Mahesh"
                  "Rahul" "Rohit" "Shyam" "Yash"},
   :transgender #{"Bharathi" "Madhu" "Manabi" "Anjum"
                  "Vani" "Riya" "Shreya" "Kiran" "Amit"}
   :last-name   #{"Lamba" "Bahl" "Sodhi" "Sardana" "Puri" "Chhabra"
                  "Malhotra" "Garewal" "Dhillon"}})
