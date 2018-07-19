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

(defn has-drug? [active-patient drug-id]
  (contains?
   (get-in active-patient [:prescription-drugs :protocol-drugs :drug-ids])
   drug-id))

(defn capsule [active-patient {:keys [id drug-dosage]}]
  (let [active? (has-drug? active-patient id)]
    [c/touchable-opacity
     {:on-press #(dispatch [:save-drug id (if active? :remove :add)])
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
      drug-dosage]]))

(defn handle-delete [id]
  (c/alert
   "Delete medicine"
   "Are you sure you want to delete the medicine?"
   [{:text "No"}
    {:text "Yes"
     :onPress #(dispatch [:remove-custom-drug id])}]
   {:cancelable false}))

(defn custom-drugs-list [{:keys [custom-drugs]}]
  [c/view
   (for [[i {:keys [drug-name drug-dosage id]}]
         (map-indexed (fn [i d] [i d]) (vals custom-drugs))
         :let [first? (= i 0)
               last? (= (inc i) (count (vals custom-drugs)))]]
     ^{:key (str (random-uuid))}
     [c/view {:style {:flex-direction "row"
                      :justify-content "space-between"
                      :align-items "center"
                      :margin-top (if first? 16 0)
                      :border-top-width (if first? 1 0)
                      :border-bottom-width 1
                      :padding-top 16
                      :padding-bottom 16
                      :border-color (s/colors :border)}}
      [c/text {:style {:font-size 20
                       :color (s/colors :primary-text)}}
       (string/capitalize drug-name)]
      [c/text {:style {:font-size 20
                       :color (s/colors :primary-text)}}
       (string/capitalize drug-dosage)]
      [c/touchable-opacity {:on-press #(handle-delete id)}
       [c/micon {:name "delete" :size 24}]]])])

(defn drugs-list [{:keys [prescription-drugs] :as active-patient}]
  (let [rows (map (fn [i d] [i d]) (range) db/protocol-drugs)]
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
         (string/capitalize (name drug-name))]
        [c/view {:flex-direction "row"}
         (for [drug drugs-with-dosages]
           ^{:key (str (random-uuid))}
           [capsule active-patient drug])]])
     [custom-drugs-list prescription-drugs]]))

(defn page []
  (let [active-patient-id (subscribe [:active-patient-id])
        active-patient (subscribe [:patients @active-patient-id])]
    (fn []
      [c/view
       [header @active-patient]
       [drugs-list @active-patient]
       [c/shadow-line]
       [c/view {:style {:margin-horizontal 32}}
        [c/action-button
         "local-pharmacy"
         :regular
         "Add another medicine"
         #(c/alert "not implemented yet")]]])))
