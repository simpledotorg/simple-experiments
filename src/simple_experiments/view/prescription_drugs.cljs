(ns simple-experiments.view.prescription-drugs
  (:require
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [clojure.string :as string]
   [clojure.set :as set]
   [simple-experiments.view.components :as c]
   [simple-experiments.view.styles :as s]
   [simple-experiments.db.patient :as db]))

(defn header [{:keys [full-name age gender street-name
                      village-or-colony phone-number]}]
  [c/view {:style {:flex-direction "row"
                   :background-color (s/colors :primary)
                   :padding-horizontal 16
                   :padding-vertical 20
                   :align-items "flex-start"
                   :elevation 10}}
   [c/touchable-opacity
    {:on-press #(dispatch [:goto :patient-summary])}
    [c/micon {:name "arrow-back"
              :size 28
              :color (s/colors :white)
              :style {:margin-right 16
                      :margin-top 2}}]]
   [c/text
    {:style {:color "white"
             :font-size 24}}
    "BP Medicines"]])

(defn row-data [prescription-drugs]
  (let [drugs-set (set (map :drug-details prescription-drugs))
        active (set/intersection drugs-set db/protocol-drugs)
        inactive (set/difference db/protocol-drugs drugs-set)]
    (->> (concat
          (map #(assoc % :active? false) (sort-by db/protocol-drug-stages < inactive))
          (map #(assoc % :active? true) (sort-by db/protocol-drug-stages < active)))
         (group-by :drug-name)
         (sort-by #(db/protocol-drug-name-stages (first %)) <))))

(defn capsule [{:keys [drug-name drug-dosage active?]}]
  [c/touchable-opacity
   {:on-press #(c/alert "dunno")
    :style {:flex-direction "row"
            :margin-left 10
            :border-radius 20
            :justify-content "center"
            :align-items "center"
            :padding-horizontal 20
            :padding-vertical 8
            :width 110
            :background-color (if active?
                                (s/colors :accent)
                                (s/colors :pale-gray))}}
   (when active?
     [c/micon {:name "check"
               :size 20
               :color (s/colors :white)
               :style {:margin-right 5}}])
   [c/text
    {:style {:font-size 18
             :color (if active?
                      (s/colors :white)
                      (s/colors :primary-text))}}
    drug-dosage]])

(defn drugs-list [prescription-drugs]
  (let [rows (map (fn [i d] [i d]) (range) (row-data prescription-drugs))]
    [c/view {:style {:padding 16
                     :margin-top 8}}
     (for [[i [drug-name drugs-with-dosages]] rows
           :let [first? (= i 0)
                 last? (= (inc i) (count rows))]]
       ^{:key (str (random-uuid))}
       [c/view {:style {:flex-direction "row"
                        :justify-content "space-between"
                        :align-items "center"
                        :border-bottom-width (if last? 0 1)
                        :padding-top (if first? 0 16)
                        :padding-bottom (if last? 0 16)
                        :border-bottom-color (s/colors :border)}}
        [c/text
         {:style {:font-size 20}}
         drug-name]
        [c/view {:flex-direction "row"}
         (for [{:keys [active? drug-dosage] :as drug} drugs-with-dosages]
           ^{:key (str (random-uuid))}
           [capsule drug])]])]))

(defn page []
  (let [active-patient-id (subscribe [:active-patient-id])
        active-patient (subscribe [:patients @active-patient-id])]
    (fn []
      [c/view
       [header @active-patient]
       [drugs-list (:prescription-drugs @active-patient)]
       [c/shadow-line]
       [c/view {:style {:margin-horizontal 32}}
        [c/action-button
         "local-pharmacy"
         :regular
         "Add another medicine"
         #(c/alert "not implemented yet")]]])))
