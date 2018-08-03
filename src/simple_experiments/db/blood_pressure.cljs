(ns simple-experiments.db.blood-pressure)

(def risk-levels
  {:normal          {:numeric 1 :display ""}
   :mildly-high     {:numeric 2 :display ""}
   :low             {:numeric 3 :display ""}
   :moderately-high {:numeric 4 :display "High"}
   :very-high       {:numeric 5 :display "High"}
   :extremely-high  {:numeric 6 :display "High"}})

(defn systolic-risk-level [systolic]
  (cond
    (<= systolic 89)      :low
    (<= 90 systolic 129)  :normal
    (<= 130 systolic 139) :mildly-high
    (<= 140 systolic 159) :moderately-high
    (<= 160 systolic 199) :very-high
    (>= systolic 200)     :extremely-high))

(defn diastolic-risk-level [diastolic]
  (cond
    (<= diastolic 59)      :low
    (<= 60 diastolic 79)   :normal
    (<= 80 diastolic 89)   :mildly-high
    (<= 90 diastolic 99)   :moderately-high
    (<= 100 diastolic 119) :very-high
    (>= diastolic 120)     :extremely-high))

(defn risk-level [{:keys [systolic diastolic]}]
  (->> [(systolic-risk-level systolic)
        (diastolic-risk-level diastolic)]
       (map risk-levels)
       (sort-by :numeric)
       (last)))
