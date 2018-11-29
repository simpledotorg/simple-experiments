(ns simple-experiments.db.seed.spec
  (:require [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [simple-experiments.events.utils :as u]
            [clojure.test.check.generators :as tcgen]
            [simple-experiments.db.seed.data :as data]))

(s/def ::full-name
  (s/with-gen
    (s/and string? not-empty)
    #(gen/fmap (fn [[fname lname]] (str fname " " lname))
               (tcgen/tuple
                (tcgen/elements (apply concat (vals (dissoc data/common-names :last-name))))
                (tcgen/elements (:last-name data/common-names))))))

(s/def ::gender
  #{"male" "female" "transgender"})

(s/def ::profile
  (s/with-gen
    keyword?
    #(gen/elements (keys data/blood-pressure-profiles))))

(s/def ::phone-number
  (s/with-gen
    (s/and string? not-empty #(re-find #"^\d*$" %))
    #(gen/fmap str (gen/choose 6000000000 9999999999))))

(s/def ::village-or-colony
  (s/with-gen
    int?
    #(gen/choose 0 4)))

(s/def ::age
  (s/with-gen
    int?
    #(gen/choose 0 100)))

(s/def ::next-visit-in-days
  (s/with-gen
    int?
    #(gen/choose 31 60)))

(s/def ::called-at
  (s/with-gen
    (s/nilable int?)
    #(gen/fmap u/days-ago (gen/choose 0 10))))

(s/def ::card-uuids
  (s/coll-of uuid? :kind set? :max-count 3 :min-count 1))

(s/def ::patient
  (s/keys :req-un [::full-name
                   ::gender
                   ::profile
                   ::phone-number
                   ::village-or-colony
                   ::age
                   ::next-visit-in-days
                   ::called-at
                   ::card-uuids]))
