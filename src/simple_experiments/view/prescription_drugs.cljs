(ns simple-experiments.view.prescription-drugs
  (:require
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [clojure.string :as string]
   [clojure.set :as set]
   [simple-experiments.view.components :as c]
   [simple-experiments.view.styles :as s]
   [simple-experiments.db.patient :as db]))

(defn header [{:keys [full-name age gender village-or-colony phone-number]}]
  [c/view {:style {:flex-direction "row"
                   :background-color (s/colors :primary)
                   :padding-horizontal 16
                   :padding-vertical 20
                   :align-items "flex-start"
                   :elevation 4}}
   [c/touchable-opacity
    {:on-press #(dispatch [:go-back])}
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
  (let [active? (has-drug? active-patient id)
        size (* 0.26 (:width c/dimensions))]
    [c/touchable-opacity
     {:on-press #(dispatch [:save-drug id (if active? :remove :add)])
      :style    {:flex-direction     "row"
                 :margin-left        10
                 :border-radius      20
                 :justify-content    "center"
                 :align-items        "center"
                 :padding-horizontal 20
                 :padding-vertical   8
                 :width              size
                 :background-color   (if active?
                                       (s/colors :accent)
                                       (s/colors :pale-gray))}}
     (when active?
       [c/micon {:name  "check"
                 :size  20
                 :color (s/colors :white)
                 :style {:margin-right 5}}])
     [c/text
      {:style {:font-size 14
               :color     (if active?
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

(defn new-custom-drug-input [type props]
  [c/text-input
   (merge
    {:style {:font-size 20
             :flex 1
             :margin-vertical 20
             :margin-horizontal 10}
     :on-change-text #(dispatch [:set-new-custom-drug type %])
     :on-submit-editing #(dispatch [:save-custom-drug])}
    props)])

(defn custom-drug-sheet []
  (let [ui-custom-drug (subscribe [:ui-custom-drug])]
    (fn []
      [c/bottom-sheet
       {:height 100
        :close-action #(dispatch [:hide-custom-drug-sheet])
        :visible? (true? (:visible? @ui-custom-drug))}

       [c/view {:style {:flex-direction "row"
                        :flex 1}}
        [new-custom-drug-input :drug-name
         {:auto-focus (:visible? @ui-custom-drug)
          :placeholder "Drug name"}]
        [new-custom-drug-input :drug-dosage
         {:placeholder "Drug dosage"}]]])))

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
                      :border-bottom-width (if last? 0 1)
                      :padding-top 16
                      :padding-bottom (if last? 0 16)
                      :border-color (s/colors :border)}}
      [c/text {:style {:font-size 18}}
       (string/capitalize drug-name)]
      [c/text {:style {:font-size 18}}
       (string/capitalize (or drug-dosage ""))]
      [c/touchable-opacity {:on-press #(handle-delete id)}
       [c/micon {:name "delete" :size 24}]]])])

(defn drugs-list [{:keys [prescription-drugs] :as active-patient}]
  (let [rows (map (fn [i d] [i d]) (range) db/protocol-drugs)]
    [c/view {:style {:padding 10
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
         {:style {:font-size 18}}
         (string/capitalize (name drug-name))]
        [c/view {:flex-direction "row"}
         (for [drug drugs-with-dosages]
           ^{:key (str (random-uuid))}
           [capsule active-patient drug])]])
     [custom-drugs-list prescription-drugs]]))

(defn done-button [style]
  [c/view
   {:style (merge {:height 90
                  :elevation 20
                  :background-color (s/colors :sheet-background)
                  :justify-content "center"}
                  style)}
   [c/floating-button
    {:title "Done"
     :on-press #(dispatch [:go-back])
     :style {:height 48
             :margin-horizontal 48
             :border-radius 3
             :elevation 1
             :font-weight "500"
             :font-size 18}}]])

(defn page []
  (let [active-patient-id (subscribe [:active-patient-id])
        active-patient (subscribe [:patients @active-patient-id])]
    (fn []
      [c/view
       {:style {:flex 1
                :background-color (s/colors :white)}}
       [header @active-patient]
       [drugs-list @active-patient]
       [c/shadow-line]
       [custom-drug-sheet]
       [c/view {:style {:margin-horizontal 32}}
        [c/action-button-outline
         "local-pharmacy"
         :regular
         "Add another medicine"
         #(dispatch [:show-custom-drug-sheet])
         32]]
       [done-button {:position "absolute"
                     :bottom 0
                     :width "100%"}]])))
