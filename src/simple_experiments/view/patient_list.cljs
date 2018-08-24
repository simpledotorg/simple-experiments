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
            [simple-experiments.events.search :as search]))

(defn last-visit [{:keys [blood-pressures] :as patient}]
  (some-> (apply max (map :created-at blood-pressures))
          timec/from-long
          (time/interval (time/now))
          time/in-days))

(defn patient-row [{:keys [full-name gender age phone-number
                           village-or-colony] :as patient}
                   last?]
  (let [visit-days-ago (last-visit patient)]
    [c/view {:ref       (fn [com]
                          (when last?
                            (dispatch [:set-last-result-ref com])))
             :on-layout #(dispatch [:compute-last-result-bottom])
             :style     {:flex-direction      "column"
                         :padding             20
                         :padding-bottom      10
                         :border-bottom-width 1
                         :border-bottom-color (s/colors :border)
                         :background-color    "white"}}

     [c/patient-data-row
      [c/text
       {:style {:color        (s/colors :primary-text)
                :font-size    16
                :margin-right 10}}
       full-name]]

     [c/patient-data-row
      [c/icon-and-text "person" (string/capitalize gender)]
      [c/icon-and-text "cake" (gstring/format "24-Mar-2975 (Age %s)" age)]]

     [c/patient-data-row
      [c/icon-and-text "call" phone-number]
      [c/icon-and-text "home" village-or-colony]]

     (when (some? visit-days-ago)
       [c/patient-data-row
        [c/text
         {:style {:padding-vertical   2
                  :padding-horizontal 4
                  :background-color   (s/colors :pale-gray)
                  :border-radius      3
                  :margin-right       4
                  :font-size          12}}
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
        [patient-row patient last?]])]))

(defn search-area []
  (let [ui (subscribe [:ui-patient-search])]
    [c/view
     {:style {:flex-direction     "row"
              :flex               1
              :padding-horizontal 16
              :padding-top        16
              :border-color       "transparent"
              :background-color   "white"
              :elevation          4
              :max-height         (* 0.27 (:height c/dimensions))}}
     [c/touchable-opacity
      {:on-press #(dispatch [:go-back])}
      [c/micon {:name  "arrow-back"
                :size  28
                :color (s/colors :disabled)
                :style {:margin-right 16
                        :margin-top   2}}]]
     [c/view
      {:style {:flex-direction  "column"
               :flex            1
               :justify-content "flex-start"}}
      [c/text-input-layout
       {:auto-focus        (if (= :search (:mode @ui)) true false)
        :on-focus          #(dispatch [:goto-search-mode])
        :on-change-text    #(dispatch [:ui-patient-search :full-name %])
        :on-submit-editing #(dispatch [:search-patients])
        :default-value     (:full-name @ui)
        :error             (when (:show-errors? @ui) (get-in @ui [:errors :full-name]))}
       "Patient's full name"]
      [c/view
       {:style {:flex-direction "row"
                :align-items    "flex-start"}}
       [c/text-input-layout
        {:keyboard-type     "numeric"
         :on-focus          #(dispatch [:goto-search-mode])
         :on-change-text    #(dispatch [:ui-patient-search :age %])
         :on-submit-editing #(dispatch [:search-patients])
         :default-value     (:age @ui)
         :error             (when (:show-errors? @ui) (get-in @ui [:errors :age]))
         :max-length        2
         :style             {:margin-right  20
                             :margin-bottom 20
                             :max-width  "30%"
                             :font-size 14}}
        "Age"]
       [c/text
        {:style {:font-size  12
                 :max-width  "32%"
                 :flex-wrap  "wrap"
                 :font-style "italic"
                 :color      (s/colors :placeholder)}}
        "Guess age if patient is not sure"]]]]))

(defn register-sheet [empty-results?]
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
                     (dispatch [:new-patient-clear]))
      :style {:height 48
              :margin-horizontal 36
              :border-radius 3
              :elevation 1
              :font-weight "500"
              :font-size 18}}]]])

(defn coach-marks [ui ui-coach]
  (cond
    (and (= :select (:mode ui))
         (:multiple-results ui-coach))
    [coach/multiple-results
     {:width "85%"
      :top   (min (:last-result-bottom ui)
                  (* 0.7 (:height c/dimensions)))}
     (count (:results ui))]

    (and (= :select (:mode ui))
         (:single-result ui-coach))
    [coach/single-result
     {:width "85%"
      :top   (:last-result-bottom ui)}]
    :else
    nil))

(defn page []
  (let [ui (subscribe [:ui-patient-search])
        ui-coach (subscribe [:ui-coach])]
    (fn []
      [c/view
       {:style {:flex 1}}
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
          [register-sheet (empty? (:results @ui))])]

       [coach-marks @ui @ui-coach]])))
