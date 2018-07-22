(ns simple-experiments.view.patient-list
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]))

(defn patient-row [{:keys [full-name gender age phone-number
                           street-name village-or-colony]} patient]
  [c/view {:style {:flex-direction "column"
                   :padding 20
                   :padding-bottom 10
                   :border-bottom-width 1
                   :border-bottom-color (s/colors :border)
                   :background-color "white"}}
   [c/view {:style {:flex-direction "row"
                    :justify-content "space-between"}}
    [c/text
     {:style {:color (s/colors :placeholder)
              :font-size 14}}
     (str (string/capitalize full-name) ", " (string/capitalize gender))]
    [c/view {:style {:flex-direction "row"}}
     [c/text {:style {:font-size 16
                      :color (s/colors :primary-text)}}
      (str "Age: " age)]]]
   [c/text
    {:style {:font-size 16
             :color (s/colors :accent)}}
    (if street-name
      (gstring/format "%s | %s, %s" phone-number street-name village-or-colony)
      (gstring/format "%s | %s" phone-number village-or-colony))]
   [c/text
    {:style {:font-size 16
             :color (s/colors :primary-text-2)}}
    (gstring/format "LAST VISIT: %s days ago" (rand-int 30))]])

(defn search-results [results]
  [c/scroll-view
   {:on-momentum-scroll-end (fn [& args] (def *args args))}
   (for [patient results]
     ^{:key (str (random-uuid))}
     [c/touchable-opacity
      {:on-press #(do
                    (dispatch [:set-active-patient-id (:id patient)])
                    (dispatch [:show-bp-sheet]))}
      [patient-row patient]])])

(defn search-area []
  (let [ui (subscribe [:ui-patient-search])]
    (fn []
      [c/view
       {:style {:flex-direction     "row"
                :padding-horizontal 16
                :padding-top        20
                :align-items        "flex-start"
                :border-color       "transparent"
                :background-color   "white"
                :elevation          4
                :height             170}}
       [c/touchable-opacity
        {:on-press #(dispatch [:goto :home])}
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
          :default-value     (:full-name @ui)}
         "Patient's full name"]
        [c/text-input-layout
         {:keyboard-type     "numeric"
          :on-focus          #(dispatch [:goto-search-mode])
          :on-change-text    #(dispatch [:ui-patient-search :age %])
          :on-submit-editing #(dispatch [:search-patients])
          :default-value     (:age @ui)}
         "Patient's age (guess if unsure)"]]])))

(defn page []
  (let [ui (subscribe [:ui-patient-search])]
    (fn []
      [c/view {:style {:flex-direction  "column"
                       :justify-content "space-between"
                       :flex            1
                       :background-color (s/colors :window-backround)}}
       [search-area]
       (when (= :search (:mode @ui))
         [c/floating-button
          {:on-press #(when (:enable-next? @ui)
                        (dispatch [:search-patients]))
           :title    "Next"
           :style    {:background-color
                      (if (:enable-next? @ui)
                        (s/colors :accent)
                        (s/colors :disabled))}}])
       (when (= :select (:mode @ui))
         [search-results (:results @ui)])

       (when (= :select (:mode @ui))
         [c/view {:style {:height 120
                          :elevation 20
                          :background-color "white"}}
          [c/view
           [c/text
            {:style {:font-size 18
                     :color (s/colors :primary-text)
                     :text-align "center"
                     :margin-vertical 14}}
            "Can't find the patient in this list?"]
           [c/floating-button
            {:title "Register as a new patient"
             :on-press #(do (dispatch [:goto :new-patient])
                            (dispatch [:new-patient-clear]))
             :style {:height 48
                     :margin-horizontal 48
                     :border-radius 3
                     :elevation 1
                     :font-weight "500"
                     :font-size 16}}]]])])))
