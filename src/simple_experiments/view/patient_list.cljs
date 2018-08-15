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
  (-> (apply max (map :created-at blood-pressures))
      timec/from-long
      (time/interval (time/now))
      time/in-days))

(defn patient-row [{:keys [full-name gender birth-year phone-number
                           village-or-colony] :as patient}
                   last?]
  (let [visit-days-ago (last-visit patient)]
    [c/view {:ref (fn [com]
                    (when last?
                      (dispatch [:set-last-result-ref com])))
             :on-layout #(dispatch [:compute-last-result-bottom])
             :style {:flex-direction "column"
                     :padding 20
                     :padding-bottom 10
                     :border-bottom-width 1
                     :border-bottom-color (s/colors :border)
                     :background-color "white"}}
     [c/view {:style {:flex-direction "row"
                      :justify-content "space-between"}}
      [c/text
       {:style {:color (s/colors :placeholder)
                :font-size 16
                :margin-bottom 4}}
       (str full-name ", " (string/capitalize gender))]
      [c/view {:style {:flex-direction "row"}}
       [c/text {:style {:font-size 16
                        :color (s/colors :primary-text)}}
        birth-year]]]
     [c/text
      {:style {:font-size 16
               :color (s/colors :accent)
               :margin-bottom 4}}
      (if (not (string/blank? phone-number))
        (gstring/format "%s | %s" phone-number village-or-colony)
        village-or-colony)]
     [c/text
      {:style {:font-size 16
               :color (s/colors :primary-text-2)}}
      (gstring/format "LAST VISIT: %s" (u/days-ago-text visit-days-ago))]]))

(defn empty-search-results []
  [c/view
   {:style {:align-items "center"
            :margin-top  100
            :opacity 0.4}}
   [c/text
    {:style {:font-size 28
             :color     (s/colors :disabled)}}
    "No Patients Match"]
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
              :padding-horizontal 16
              :padding-top        20
              :border-color       "transparent"
              :background-color   "white"
              :elevation          4
              :height             190}}
     [c/touchable-opacity
      {:on-press #(dispatch [:go-back])}
      [c/micon {:name  "arrow-back"
                :size  28
                :color (s/colors :disabled)
                :style {:margin-right 16
                        :margin-top   2}}]]
     [c/view
      {:style {:flex-direction "column"
               :flex           1}}
      [c/text-input-layout
       {:auto-focus        (if (= :search (:mode @ui)) true false)
        :on-focus          #(dispatch [:goto-search-mode])
        :on-change-text    #(dispatch [:ui-patient-search :full-name %])
        :on-submit-editing #(dispatch [:search-patients])
        :default-value     (:full-name @ui)
        :error             (when (:show-errors? @ui) (get-in @ui [:errors :full-name]))}
       "Patient's full name"]
      [c/text-input-layout
       {:keyboard-type     "numeric"
        :on-focus          #(dispatch [:goto-search-mode])
        :on-change-text    #(dispatch [:ui-patient-search :birth-year %])
        :on-submit-editing #(dispatch [:search-patients])
        :default-value     (:birth-year @ui)
        :error             (when (:show-errors? @ui) (get-in @ui [:errors :birth-year]))
        :max-length        4}
       "Birth year (guess if unsure)"]]]))

(defn register-sheet [empty-results?]
  [c/view {:style {:height 120
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
     {:title "Register patient"
      :on-press #(do (dispatch [:goto :new-patient])
                     (dispatch [:new-patient-clear]))
      :style {:height 48
              :margin-horizontal 48
              :border-radius 3
              :elevation 1
              :font-weight "500"
              :font-size 18}}]]])

(defn coach-marks [ui ui-coach]
  (cond
    (and (= :select (:mode ui))
         (:multiple-results ui-coach))
    [coach/multiple-results
     {:top (:last-result-bottom ui)
      :width "75%"}]

    (and (= :select (:mode ui))
         (:single-result ui-coach))
    [coach/single-result
     {:top (:last-result-bottom ui)
      :width "75%"}]

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

        (when (= :select (:mode @ui))
          [register-sheet (empty? (:results @ui))])]

       [coach-marks @ui @ui-coach]])))
