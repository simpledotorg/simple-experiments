(ns simple-experiments.db.patient
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.test.check]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [cljs-time.format :as timef]
            [clojure.test.check.generators :as tcgen]
            [clojure.spec.gen.alpha :as gen]))

(def common-names
  {:female      #{"anjali" "divya" "ishita" "priya" "priyanka"
                  "riya" "shreya" "tanvi" "tanya" "vani"},
   :male        #{"abhishek" "adityaamit" "ankit" "deepak" "mahesh"
                  "rahul" "rohit" "shyam" "yash"},
   :transgender #{"bharathi" "madhu" "manabi" "anjum"
                  "vani" "riya" "shreya" "kiran" "amit"}
   :last-name   #{"Lamba" "Bahl" "Sodhi" "Sardana" "Puri" "Chhabra"
                  "Malhotra" "Garewal" "Dhillon"}})

(def common-addresses
  {:street-name
   ["Bhagat singh colony" "Gandhi Basti" "NFL Colony" "Farid Nagari"
    "Bathinda Road" "Bus Stand Rd" "Hirke Road" "Makhewala Jhanduke Road"]
   :village-or-colony
   ["Bathinda" "Bhagwangarh" "Dannewala" "Nandgarh" "Nathana"
    "Bhikhi" "Budhlada" "Hirke" "Jhanduke" "Mansa" "Bareta"
    "Bhaini" "Bagha" "Sadulgarh" "Sardulewala"]})

(def protocol-drugs
  [[:amlodipine [{:id :am-5 :drug-dosage "5mg"}
                 {:id :am-10 :drug-dosage "10mg"}]]
   [:telmisartan [{:id :tel-40 :drug-dosage "40mg"}
                  {:id :tel-80 :drug-dosage "80mg"}]]
   [:chlorthalidone [{:id :chlo-12-5 :drug-dosage "12.5mg"}
                     {:id :chlo-25 :drug-dosage "25mg"}]]])

(def protocol-drugs-by-id
  (->> (for [[drug-name drugs] protocol-drugs
             {:keys [id drug-dosage]} drugs]
         {id {:drug-name drug-name :drug-dosage drug-dosage}})
       (into {})))

(def protocol-drug-ids
  (set (keys protocol-drugs-by-id)))

(s/def ::non-empty-string
  (s/and string? not-empty))

(s/def ::id
  (s/with-gen
    (s/and string? #(= 36 (count %)))
    #(gen/fmap (fn [x] (str x)) (gen/uuid))))

(s/def ::gender
  #{"male" "female" "transgender"})

(s/def ::full-name
  (s/with-gen
    (s/and string? not-empty)
    #(gen/fmap (fn [x] (string/join " " (take 2 x)))
               (tcgen/shuffle (apply concat (vals common-names))))))

(s/def ::status
  #{"active" "dead" "migrated" "unresponsive" "inactive"})

(s/def ::timestamp
  (s/with-gen
    int?
    #(gen/choose (timec/to-long (time/date-time 1920 0 0))
                 (timec/to-long (time/now)))))

(s/def ::recent-timestamp
  (s/with-gen
    int?
    #(gen/choose (timec/to-long (time/minus (time/now) (time/days 50)))
                 (timec/to-long (time/now)))))

(s/def ::age
  (s/with-gen
    (s/and int? #(< 0 % 100))
    #(gen/choose 18 90)))

(s/def ::age-string
  (s/and string? #(<= 0 (js/parseInt %) 100)))

(s/def ::date-of-birth-string
  (s/and string?
         #(= (count %) 10)
         #(try
            (time/before? (timef/parse (timef/formatter "dd/MM/YYYY") %)
                          (time/now))
            (catch :default e false))))

(s/def ::optional-dob-string
  (s/nilable ::date-of-birth-string))

(s/def ::date-of-birth ::timestamp)

(s/def ::age-updated-at ::recent-timestamp)

(s/def ::created-at ::recent-timestamp)

(s/def ::updated-at ::recent-timestamp)

(s/def ::deleted-at ::recent-timestamp)

(s/def ::phone-number
  (s/with-gen
    (s/and string? not-empty #(re-find #"^\d*$" %))
    #(gen/fmap str (gen/choose 6000000000 9999999999))))

(s/def ::village-or-colony
  (s/with-gen
    (s/and string? not-empty)
    #(gen/elements (:village-or-colony common-addresses))))

(s/def ::systolic
  (s/with-gen
    int?
    #(gen/choose 50 300)))

(s/def ::diastolic
  (s/with-gen
    int?
    #(gen/choose 50 100)))

(s/def ::blood-pressure
  (s/keys :req-un [::systolic ::diastolic ::created-at ::updated-at]))

(s/def ::blood-pressures
  (s/with-gen
    (s/coll-of ::blood-pressure)
    #(gen/vector (s/gen ::blood-pressure) 0 5)))

(s/def ::drug-name
  (s/with-gen
    (and string? not-empty)
    #(gen/elements #{"randomzole" "lisinopril" "losartan"
                     "furosemide" "customoril" "cherries"})))

(s/def ::drug-dosage
  (s/with-gen
    (and string? not-empty)
    #(gen/elements #{"10mg" "5mg" "1 packet"
                     "10 times" "12.5g" "20mg"})))

(s/def ::custom-drug
  (s/keys :req-un [::id ::drug-name ::drug-dosage ::created-at ::updated-at]))

(s/def ::custom-drugs
  (s/with-gen
    (s/map-of ::id ::custom-drug)
    #(gen/fmap
      (fn [ds] (zipmap (map :id ds) ds))
      (gen/vector (s/gen ::custom-drug) 0 2))))

(s/def ::drug-ids
  (s/with-gen
    set?
    #(gen/set (gen/elements protocol-drug-ids)
              {:min-elements 0
               :max-elements 3})))

(s/def ::protocol-drugs
  (s/keys :req-un [::updated-at ::drug-ids]))

(s/def ::prescription-drugs
  (s/keys :req-un [::protocol-drugs ::custom-drugs]))

(def patient-fields
  #{:id :full-name :age :phone-number :gender
    :date-of-birth :village-or-colony :district :state})

(s/def ::patient
  (s/keys :req-un [::id ::gender ::full-name ::status ::date-of-birth
                   ::age ::age-updated-at ::created-at ::updated-at
                   ::phone-number ::village-or-colony
                   ::blood-pressures ::prescription-drugs]))

(defn generate-patients [num-patients]
  (let [patients (gen/sample (s/gen ::patient) num-patients)]
    (zipmap (map :id patients)
            patients)))

(comment
  ;;gen patients
  (dotimes [_ 10]
    (re-frame.core/dispatch [:add-patient (gen/generate (s/gen ::patient))]))
  )
