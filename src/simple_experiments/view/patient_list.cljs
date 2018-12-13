(ns simple-experiments.view.patient-list
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.common :as com]
            [simple-experiments.view.styles :as s]
            [simple-experiments.events.utils :as u]
            [simple-experiments.view.coach :as coach]
            [simple-experiments.events.search :as search]
            [simple-experiments.events.simple-card :as simple-card]))

(defn last-visit [{:keys [blood-pressures] :as patient}]
  (some-> (apply max (map :created-at blood-pressures))
          timec/from-long
          (time/interval (time/now))
          time/in-days))

(defn patient-row [{:keys [full-name gender age phone-number
                           date-of-birth village-or-colony] :as patient}
                   last?]
  (let [visit-days-ago (last-visit patient)
        text-style     {:color         (s/colors :light-text)
                        :font-size     14
                        :margin-right  10}]
    [c/view {:ref       (fn [com]
                          (when last?
                            (dispatch [:set-ref :last-search-result com])))
             :on-layout #(dispatch [:measure :last-search-result])
             :style     {:flex-direction      "column"
                         :padding             20
                         :padding-bottom      10
                         :border-bottom-width 1
                         :border-bottom-color (s/colors :border)
                         :background-color    "white"}}

     [c/patient-data-row
      [c/text
       {:style {:color        (s/colors :accent)
                :font-size    16
                :margin-right 10}}
       (str full-name ", " (string/capitalize gender) ", " age)]]

     [c/patient-data-row
      [c/text
       {:style text-style}
       (if (some? date-of-birth)
         (u/dob->dob-string date-of-birth)
         (u/age->dob-string age))]

      (when (some? phone-number)
        [c/micon
         {:name  "call"
          :size  14
          :style {:border-radius 3
                  :margin-right  4}}])
      (when (some? phone-number)
        [c/text
         {:style text-style}
         (u/obfuscate phone-number)])]

     (when (some? village-or-colony)
       [c/patient-data-row
        [c/text
         {:style text-style}
         village-or-colony]])

     (when (some? visit-days-ago)
       [c/patient-data-row
        [c/text
         {:style {:padding-vertical   1
                  :padding-horizontal 4
                  :background-color   (s/colors :pale-gray)
                  :border-radius      3
                  :margin-right       4
                  :font-size          10}}
         "LAST VISIT"]
        [c/text
         {:style {:font-size 14
                  :color     (s/colors :light-text)}}
         (u/days-ago-text visit-days-ago)]])]))

(defn empty-search-results []
  [c/view
   {:style {:align-items "center"
            :margin-top  (* 0.12 (:height c/dimensions))
            :opacity 0.4}}
   [c/text
    {:style {:font-size 28
             :color     (s/colors :disabled)}}
    "No patients match"]
   [c/view {:style {:margin-top       10
                    :width            1
                    :height           130
                    :background-color (s/colors :disabled)}}]
   [c/view
    {:style {:position "absolute"
             :bottom -12}}
    [c/miconx {:name  "menu-down"
               :size  32
               :color (s/colors :disabled)}]]])

(defn active-card-status [patient active-card]
  (let [{:keys [uuid six-digit-id]} active-card]
    (cond
      (some? uuid)
      :pending-association

      (simple-card/has-six-digit-id? patient six-digit-id)
      :associated

      (simple-card/pending-association? active-card)
      :pending-association

      (some? active-card)
      :pending

      :else
      nil)))

(defn search-results [results]
  (let [active-card (subscribe [:active-card])]
    (fn []
      (if (empty? results)
        [empty-search-results]
        [c/scroll-view
         (for [[i patient] (map-indexed (fn [i r] [i r]) results)
               :let [last? (= (inc i) (count results))]]
           ^{:key (str (random-uuid))}
           [c/touchable-opacity
            {:on-press #(do (when-let [status (active-card-status patient @active-card)]
                              (dispatch [:set-active-card
                                         (:uuid @active-card)
                                         (:six-digit-id @active-card)
                                         status]))
                            (dispatch [:set-active-patient-id (:id patient)])
                            (dispatch [:show-bp-sheet]))}
            [patient-row patient last?]])]))))

(defn register-sheet [empty-results?]
  (let [active-card (subscribe [:active-card])]
    (fn []
      [c/view {:style {:height "20%"
                       :elevation 20
                       :background-color (s/colors :sheet-background)}}
       [c/view
        [c/text
         {:style {:font-size 18
                  :color (s/colors :primary-text)
                  :text-align "center"
                  :margin-vertical 14}}
         (if empty-results?
           "Patient is not registered."
           "Can't find the patient in this list?")]
        [c/floating-button
         {:title "Register as a new patient"
          :on-press #(do (dispatch [:goto :new-patient])
                         (dispatch [:new-patient-clear])
                         (when (simple-card/pending? @active-card)
                           (dispatch [:set-active-card
                                      (:uuid @active-card)
                                      (:six-digit-id @active-card)
                                      :pending-registration])))
          :style {:height 48
                  :margin-horizontal 36
                  :border-radius 3
                  :elevation 1
                  :font-weight "500"
                  :font-size 18}}]]])))

(defn coach-marks [ui ui-coach]
  (let [position (subscribe [:ui-measurements :last-search-result :bottom])]
    (fn [ui ui-coach]
      (cond
        (:multiple-results ui-coach)
        [coach/multiple-results
         {:width "85%"
          :top   (min @position (* 0.6 (:height c/dimensions)))}
         (:full-name ui)]

        (:single-result ui-coach)
        [coach/single-result
         {:width "85%"
          :top   @position}
         (:full-name ui)]
        :else
        nil))))

(defn search-by-name-header []
  (let [ui (subscribe [:ui-patient-search])]
    (fn []
      [c/touchable-opacity
       {:style {:background-color (s/colors :white)
                :elevation 2
                :margin-bottom 8}
        :on-press #(dispatch [:go-back])}
       [c/view
        {:style {:flex-direction     "row"
                 :padding-horizontal 16
                 :padding-vertical   20
                 :justify-content "space-between"}}
        [c/view
         {:style {:flex-direction "row"
                  :align-items "center"}}
         [c/micon {:name  "arrow-back"
                   :size  24
                   :color (s/colors :disabled)
                   :style {:margin-right 16
                           :margin-top   2}}]
         [c/text {:style {:font-size 18
                          :color     (s/colors :primary-text)}}
          (u/title-case (:full-name @ui))]]
        [c/view
         {:style {:align-items "center"
                  :flex-direction "row"}}
         [c/text {:style {:font-size 12
                          :color (s/colors :light-text)
                          :font-weight "bold"
                          :margin-right 8}}
          "AGE"]
         [c/text
          {:style {:font-size 18
                   :color (s/colors :primary-text)}}
          (:age @ui)]]]])))

(defn search-by-six-digit-id-header []
  (let [active-card (subscribe [:active-card])]
    (fn []
      [c/header
       [c/text
        {:style {:letter-spacing 2}}
        (:six-digit-display @active-card)]])))

(defn page []
  (let [ui (subscribe [:ui-patient-search])
        active-card (subscribe [:active-card])
        ui-coach (subscribe [:ui-coach])]
    (fn []
      [c/view
       {:style {:flex 1}}
       [c/view
        {:style {:flex 1
                 :background-color (s/colors :window-backround)}}
        (cond
          (#{:pending :pending-association :pending-registration} (:status @active-card))
          [com/add-to-patient-header]

          (#{:pending-selection} (:status @active-card))
          [search-by-six-digit-id-header]

          :else
          [search-by-name-header])
        [c/view {:style {:flex-direction  "column"
                         :justify-content "space-between"
                         :flex            1
                         :background-color (s/colors :window-backround)}}
         [search-results (:results @ui)]

         (when (not (:multiple-results @ui-coach))
           [register-sheet (empty? (:results @ui))])]]

       [coach-marks @ui @ui-coach]])))
