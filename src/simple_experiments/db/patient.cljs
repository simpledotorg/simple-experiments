(ns simple-experiments.db.patient
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.test.check]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.test.check.generators :as tcgen]
            [clojure.spec.gen.alpha :as gen]))

(def common-names
  {:female      #{"anjali" "divya" "ishita" "priya" "priyanka"
                  "riya" "shreya" "tanvi" "tanya" "vani"},
   :male        #{"abhishek" "adityaamit" "ankit" "deepak" "mahesh"
                  "rahul" "rohit" "shyam" "yash"},
   :transgender #{"bharathi" "madhu" "manabi" "anjum"
                  "vani" "riya" "shreya" "kiran" "amit"}})

(def common-addresses
  {:street-name
   ["Bhagat singh colony" "Gandhi Basti" "NFL Colony" "Farid Nagari"
    "Bathinda Road" "Bus Stand Rd" "Hirke Road" "Makhewala Jhanduke Road"]
   :village-or-colony
   ["Bathinda" "Bhagwangarh" "Dannewala" "Nandgarh" "Nathana"
    "Bhikhi" "Budhlada" "Hirke" "Jhanduke" "Mansa" "Bareta"
    "Bhaini" "Bagha" "Sadulgarh" "Sardulewala"]})

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
                 (time/now))))

(s/def ::age
  (s/and int? #(< 0 % 100)))

(s/def ::date-of-birth ::timestamp)

(s/def ::age-updated-at ::timestamp)

(s/def ::created-at ::timestamp)

(s/def ::updated-at ::timestamp)

(s/def ::phone-number
  (s/with-gen
    (s/and string? not-empty)
    #(gen/fmap str (gen/choose 6000000000 9999999999))))

(s/def ::street-name
  (s/with-gen
    (s/and string? not-empty)
    #(gen/elements (:street-name common-addresses))))

(s/def ::village-or-colony
  (s/with-gen
    (s/and string? not-empty)
    #(gen/elements (:village-or-colony common-addresses))))

(s/def ::patient
  (s/keys :req-un [::id ::gender ::full-name ::status ::date-of-birth
                   ::age ::age-updated-at ::created-at ::updated-at
                   ::phone-number ::street-name ::village-or-colony]))

(comment
  ;;gen patients
  (dotimes [_ 10]
    (re-frame.core/dispatch [:add-patient (gen/generate (s/gen ::patient))]))
  )
