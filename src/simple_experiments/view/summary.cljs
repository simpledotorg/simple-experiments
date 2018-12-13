(ns simple-experiments.view.summary
  (:require
   [reagent.core :as r]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [cljs-time.core :as time]
   [cljs-time.coerce :as timec]
   [clojure.string :as string]
   [goog.string :as gstring]
   [goog.string.format]
   [simple-experiments.db.patient :as db]
   [simple-experiments.view.bp :as bp]
   [simple-experiments.view.coach :as coach]
   [simple-experiments.view.components :as c]
   [simple-experiments.view.styles :as s]
   [simple-experiments.events.utils :as u]
   [simple-experiments.events.simple-card :as simple-card]))

(defn any-drugs? [drugs]
  (or (not-empty (get-in drugs [:protocol-drugs :drug-ids]))
      (not-empty (:custom-drug drugs))))

(defn summary-header [{:keys [full-name age gender date-of-birth
                              village-or-colony phone-number]}]
  (let [icon-style {:style {:background-color (s/colors :disabled)
                            :opacity          0.5}
                    :color (s/colors :white)}
        text-style {:color        (s/colors :white)
                    :font-size    14
                    :margin-right 12}]
    [c/view {:style {:flex-direction     "row"
                     :background-color   (s/colors :primary)
                     :padding-horizontal 16
                     :padding-vertical   20
                     :align-items        "flex-start"
                     :justify-content    "flex-start"
                     :elevation          10}}
     [c/touchable-opacity
      {:on-press #(dispatch [:go-back])}
      [c/micon {:name  "arrow-back"
                :size  24
                :color (s/colors :white)
                :style {:margin-right 16
                        :margin-top   4
                        :padding-bottom "15%"}}]]
     [c/view
      {:style {:flex-direction "column"}}
      [c/text
       {:style {:color         "white"
                :font-size     20
                :margin-bottom 4}}
       (str full-name ", " age)]
      [c/patient-data-row
       [c/text {:style text-style}
        (if (some? date-of-birth)
          (u/dob->dob-string date-of-birth)
          (u/age->dob-string age))]

       (when (some? phone-number)
         [c/micon
          {:name  "call"
           :size  14
           :color (s/colors :white)
           :style {:border-radius 3
                   :margin-right  4}}])

       (when (some? phone-number)
         [c/text {:style text-style}
          (u/obfuscate phone-number)])]
      (when (some? village-or-colony)
        [c/patient-data-row
         [c/text {:style text-style}
          village-or-colony]])]
     [c/touchable-opacity
      {:on-press #()
       :style    {:border-radius      2
                  :border-width       1
                  :border-color       (s/colors :white)
                  :position           "absolute"
                  :right              20
                  :top                24
                  :padding-vertical   3
                  :padding-horizontal 8}}
      [c/text
       {:style {:color     "white"
                :font-size 14}}
       (string/upper-case "Edit")]]]))

(defn drug-row [{:keys [drug-name drug-dosage]}]
  [c/view {:style {:flex-direction   "row"
                   :justify-content  "flex-start"
                   :padding-vertical 6}}
   [c/text
    {:style {:font-size    16
             :margin-right 10
             :min-width    50
             :color        (s/colors :primary-text)}}
    (string/capitalize (or drug-dosage ""))]
   [c/text
    {:style {:font-size 16
             :color     (s/colors :primary-text)}}
    (string/capitalize (name drug-name))]])

(defn latest-drug-time [{:keys [custom-drugs protocol-drugs] :as drugs}]
  (->> (map :updated-at custom-drugs)
       (cons (:updated-at protocol-drugs))
       sort
       last))

(defn drugs-updated-since [drugs]
  (let [updated-days-ago (c/number-of-days-since
                          (timec/from-long (latest-drug-time drugs)))]
    [c/view {:style {:flex-direction  "column"
                     :justify-content "center"
                     :align-items     "flex-end"}}
     [c/text {:style {:font-size 12
                      :color (s/colors :placeholder)}}
      "Updated"]
     [c/text {:style {:font-size 14
                      :color (s/colors :light-text)}}
      (if (= 0 updated-days-ago)
        "Today"
        (str updated-days-ago " days ago"))]]))

(defn all-drug-details [{:keys [custom-drugs protocol-drugs] :as drugs}]
  (concat
   (map db/protocol-drugs-by-id (:drug-ids protocol-drugs))
   (vals custom-drugs)))

(defn drugs-list [drugs]
  [c/view {:style {:flex-direction  "row"
                   :justify-content "space-between"}}
   [c/view {:style {:flex-direction "column"}}
    (for [drug-details (all-drug-details drugs)]
      ^{:key (str (random-uuid))}
      [drug-row drug-details])]
   (when (any-drugs? drugs)
     [drugs-updated-since drugs])])

(defn prescription [drugs]
  [c/view {:style {:padding-horizontal 32
                   :padding-vertical 16}}
   [drugs-list drugs]
   [c/action-button-outline
    "local-pharmacy"
    :regular
    (if (any-drugs? drugs) "Update Medicines" "Add Medicines")
    #(dispatch [:goto :prescription-drugs])
    32
    :style {:margin-top (if (any-drugs? drugs) 20 0)}]])

(defn stepper []
  (let [current-step (subscribe [:ui-summary :schedule-stepper :current-step])]
    [c/view
     {:style {:flex-direction "row"
              :align-items "center"
              :justify-content "center"
              :margin-vertical 8}}
     [c/touchable-opacity
      {:on-press #(dispatch [:schedule-stepper :previous])}
      [c/micon {:name "remove-circle-outline"
                :size 24
                :color (s/colors :accent)
                :style {:padding 40}}]]
     [c/text
      {:style {:font-size 34
               :width "50%"
               :color (s/colors :primary-text)
               :text-align "center"}}
      (or @current-step "4 weeks")]
     [c/touchable-opacity
      {:on-press #(dispatch [:schedule-stepper :next])}
      [c/micon {:name "add-circle-outline"
                :size 24
                :color (s/colors :accent)
                :style {:padding 40}}]]]))

(defn schedule-sheet [active-patient]
  (let [show?      (subscribe [:ui-summary :show-schedule-sheet?])
        next-visit (subscribe [:ui-summary :next-visit])]
    (fn []
      [c/modal {:animation-type   "slide"
                :transparent      true
                :visible          (true? @show?)
                :on-request-close #(dispatch [:hide-schedule-sheet])}
       [c/view
        {:style {:flex             1
                 :background-color (s/colors :overlay-dark)}}
        [c/touchable-opacity
         {:on-press #(dispatch [:hide-schedule-sheet])
          :style    {:height          "60%"
                     :justify-content "flex-end"
                     :align-items     "center"
                     :padding-bottom  20}}]
        [c/view
         {:style {:background-color (s/colors :white)
                  :justify-content  "center"
                  :align-items      "center"
                  :flex             1
                  :padding          20
                  :border-radius    5}}
         [c/text
          {:style {:width               "100%"
                   :text-align          "center"
                   :font-size           16
                   :font-weight         "bold"
                   :color               (s/colors :primary-text)
                   :padding-bottom      10
                   :border-bottom-color (s/colors :border)
                   :border-bottom-width 1}}
          "Schedule next visit in"]

         [stepper]

         [c/view
          {:style {:flex-direction "row"}}
          [c/floating-button
           {:title    "Not now"
            :on-press #(dispatch [:summary-save])
            :style    {:flex             1
                       :background-color (s/colors :white)
                       :color            (s/colors :accent)
                       :border-color     (s/colors :accent)
                       :border-width     1
                       :height           48
                       :border-radius    3
                       :elevation        1
                       :font-size        14
                       :margin-right     16}}]
          [c/floating-button
           {:title    "Done"
            :on-press #(do (dispatch [:set-schedule])
                           (dispatch [:summary-save]))
            :style    {:flex          1
                       :height        48
                       :border-radius 3
                       :elevation     1
                       :font-size     14}}]]]]])))

(defn save-button []
  [c/view {:style {:height 70
                   :elevation 10
                   :background-color (s/colors :sheet-background)
                   :justify-content "center"}}
   [c/floating-button
    {:title "Save"
     :on-press #(dispatch [:show-schedule-sheet])
     :style {:height 42
             :margin-horizontal 48
             :border-radius 3
             :elevation 1
             :font-size 16}}]])

(defn confirm-association []
  (let [active-card (subscribe [:active-card])
        ui (subscribe [:ui-summary])
        active-patient-id (subscribe [:active-patient-id])
        text-style {:font-size 20
                    :color (s/colors :primary-text)}]
    (fn []
      [c/bottom-sheet
       {:height 136
        :visible? (simple-card/pending-association? @active-card)
        :close-action #(dispatch [:go-back])}

       [c/view
        [c/view
         {:style {:flex-direction "row"
                  :align-items "center"
                  :justify-content "center"}}
         [c/image {:resize-mode "center"
                   :source c/qr-scan-icon-gray
                   :style {:width 26
                           :max-height 70
                           :margin-right 12}}]
         [c/text
          {:style text-style}
          "Add ID "
          [c/text
           {:style (merge {:letter-spacing 1
                           :font-weight "500"}
                          text-style)}
           (:six-digit-display @active-card)]
          " to patient?"]]
        [c/view
         {:style {:flex-direction "row"
                  :align-items "center"
                  :justify-content "center"}}
         [c/button-outline
          "CANCEL"
          #(dispatch [:go-back])
          {:height 48
           :width "45%"
           :margin-horizontal 8
           :margin-bottom 16
           :border-radius 3
           :font-size 16}]
         [c/floating-button
          {:title "ADD"
           :on-press #(dispatch [:associate-simple-card-with-patient
                                 @active-card
                                 @active-patient-id])
           :style {:height 48
                   :width "45%"
                   :margin-horizontal 8
                   :margin-bottom 16
                   :border-radius 3
                   :elevation 1
                   :font-size 16}}]]]])))

(defn page []
  (let [active-patient-id (subscribe [:active-patient-id])
        active-patient    (subscribe [:patients @active-patient-id])
        ui                (subscribe [:ui-summary])
        first-bp-bottom   (subscribe [:ui-measurements :first-bp :bottom])
        coach-new-bp?     (subscribe [:ui-coach :new-bp])]
    (r/create-class
     {:component-did-mount
      (fn [] (dispatch [:clear-bp-inputs]))
      :reagent-render
      (fn []
        [c/view {:style {:flex 1}}
         [c/scroll-view
          {:sticky-header-indices [0]
           :keyboard-should-persist-taps "handled"}
          [summary-header @active-patient]
          [c/view
           {:style {:flex            1
                    :flex-direction  "column"
                    :justify-content "flex-start"
                    :margin-bottom   20}}
           [prescription (:prescription-drugs @active-patient)]
           [c/shadow-line]
           [bp/history (:blood-pressures @active-patient)]
           [bp/bp-sheet]
           [schedule-sheet @active-patient]]]
         [confirm-association]
         (when-not @coach-new-bp?
           [save-button])
         (when (and @coach-new-bp?
                    @first-bp-bottom)
           [coach/new-blood-pressure
            {:width "70%"
             :top   @first-bp-bottom}])])})))
