(ns simple-experiments.db
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
    #(satisfies? time/DateTimeProtocol %)
    #(gen/fmap (fn [x] (timec/from-long x))
               (gen/choose (timec/to-long (time/date-time 1920 0 0))
                           (time/now)))))

(s/def ::date-of-birth ::timestamp)

(s/def ::age
  (s/and int? #(< 0 % 100)))

(s/def ::age-updated-at
  string?)

(s/def ::created-at ::timestamp)

(s/def ::updated-at ::timestamp)

(s/def ::patient
  (s/keys :req-un [::id ::gender ::full-name ::status ::date-of-birth
                   ::age ::age-updated-at ::created-at ::updated-at]))

;; initial state of app-db
(def app-db
  {:active-page :home
   :home {:active-tab :patient}
   :store {:patients []}})
