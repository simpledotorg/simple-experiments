(ns simple-experiments.view.patient-list
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
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
            {:on-press #(do
                          (dispatch [:set-active-patient-id (:id patient)])
                          (dispatch [:show-bp-sheet]))}
            [patient-row patient last?]])]))))

(defn age-input []
  (let [ui (subscribe [:ui-patient-search])]
    (fn []
      [c/view
       {:style {:flex-direction "row"
                :align-items    "center"}}
       [c/text-input-layout
        {:keyboard-type     "numeric"
         :on-focus          #(dispatch [:goto-search-mode])
         :on-change-text    #(dispatch [:ui-patient-search :age %])
         :on-submit-editing #(dispatch [:search-patients])
         :default-value     (:age @ui)
         :error             (when (:show-errors? @ui) (get-in @ui [:errors :age]))
         :max-length        2
         :style             {:margin-right 20
                             :max-width "45%"}}
        "Age"]
       [c/text
        {:style {:font-size  12
                 :max-width  "32%"
                 :flex-wrap  "wrap"
                 :font-style "italic"
                 :color      (s/colors :placeholder)}}
        "Guess age if patient is not sure"]])))

(defn active-input [ui]
  (cond
    (not (string/blank? (:date-of-birth ui)))
    :date-of-birth

    (not (string/blank? (:age ui)))
    :age

    :else
    :none))

(defn age-or-dob []
  (let [ui (subscribe [:ui-patient-search])]
    (fn []
      [c/view
       {:style {:flex-direction  "row"
                :align-items     "flex-end"
                :justify-content "space-between"
                :flex            1}}
       (when (not= :date-of-birth (active-input @ui))
         [c/text-input-layout
          {:keyboard-type     "numeric"
           :on-focus          #(dispatch [:goto-search-mode])
           :on-change-text    #(dispatch [:ui-patient-search :age %])
           :on-submit-editing #(dispatch [:search-patients])
           :default-value     (:age @ui)
           :error             (when (:show-errors? @ui) (get-in @ui [:errors :age]))
           :max-length        2
           :style             {:max-width (if (= :age (active-input @ui)) "100%" "30%")}}
          "Age"])
       (when (= :none (active-input @ui))
         [c/text
          {:style {:font-size         16
                   :margin-horizontal 20
                   :margin-bottom     10}}
          "OR"])
       (when (not= :age (active-input @ui))
         [c/text-input-layout
          {:keyboard-type     "numeric"
           :on-focus          #(dispatch [:goto-search-mode])
           :on-change-text    #(dispatch [:ui-patient-search :date-of-birth %])
           :on-submit-editing #(dispatch [:search-patients])
           :default-value     (:date-of-birth @ui)
           :error             (when (:show-errors? @ui) (get-in @ui [:errors :date-of-birth]))
           :max-length        10}
          "Date of birth"])])))

(defn search-inputs []
  (let [ui      (subscribe [:ui-patient-search])
        setting (subscribe [:store-settings :age-vs-age-or-dob])]
    (fn []
      [c/view
       {:style {:flex-direction  "column"
                :flex            1
                :justify-content "flex-start"
                :margin-bottom   10}}
       [c/text-input-layout
        {:auto-focus        (if (= :search (:mode @ui)) true false)
         :on-focus          #(dispatch [:goto-search-mode])
         :on-change-text    #(dispatch [:ui-patient-search :full-name %])
         :on-submit-editing #(dispatch [:search-patients])
         :default-value     (:full-name @ui)
         :error             (when (:show-errors? @ui) (get-in @ui [:errors :full-name]))}
        "Patient's full name"]
       (if (= @setting "age")
         [age-input]
         [age-or-dob])])))

(defn search-area []
  (let [active-card (subscribe [:active-card])]
    (fn []
      [c/view
       {:style {:flex-direction     "row"
                :flex               1
                :padding-horizontal 10
                :padding-top        16
                :border-color       "transparent"
                :background-color   "white"
                :elevation          4
                :max-height         (* 0.28 (:height c/dimensions))}}
       (when-not (simple-card/pending? @active-card)
         [c/touchable-opacity
          {:on-press #(dispatch [:go-back])}
          [c/micon {:name  "arrow-back"
                    :size  24
                    :color (s/colors :disabled)
                    :style {:margin-right 10
                            :margin-top   2}}]])
       [search-inputs]])))

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
                           (dispatch [:set-active-card (:uuid @active-card) :awaiting-registration])))
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
        (and (= :select (:mode ui))
             (:multiple-results ui-coach))
        [coach/multiple-results
         {:width "85%"
          :top   (min @position (* 0.6 (:height c/dimensions)))}
         (:full-name ui)]

        (and (= :select (:mode ui))
             (:single-result ui-coach))
        [coach/single-result
         {:width "85%"
          :top   @position}
         (:full-name ui)]
        :else
        nil))))

(defn page []
  (let [ui (subscribe [:ui-patient-search])
        active-card (subscribe [:active-card])
        ui-coach (subscribe [:ui-coach])]
    (fn []
      [c/view
       {:style {:flex 1}}
       [c/view
        {:style {:flex 1}}
        (when (simple-card/pending? @active-card)
          [c/header
           [c/text "Add "
            [c/text
             {:style {:letter-spacing 2}}
             (:six-digit-display @active-card)]
            " to patient"]])
        [c/view {:style {:flex-direction  "column"
                         :justify-content "space-between"
                         :flex            1
                         :background-color (s/colors :window-backround)}}
         [search-area]
         (when (= :search (:mode @ui))
           [c/floating-button
            {:on-press #(dispatch [:search-patients])
             :title    "Next"
             :style    {:background-color
                        (if (:enable-next? @ui)
                          (s/colors :accent)
                          (s/colors :disabled))}}])
         (when (= :select (:mode @ui))
           [search-results (:results @ui)])

         (when (and (= :select (:mode @ui))
                    (not (:multiple-results @ui-coach)))
           [register-sheet (empty? (:results @ui))])]]

       [coach-marks @ui @ui-coach]])))
