(ns simple-experiments.view.patient-search
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
        {:auto-focus        true
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

(defn page []
  (let [ui (subscribe [:ui-patient-search])]
    (fn []
      [c/view
       {:style {:flex 1}}
       [c/view
        {:style {:flex 1}}
        [com/add-to-patient-header]
        [c/view {:style {:flex-direction   "column"
                         :justify-content  "space-between"
                         :flex             1
                         :background-color (s/colors :window-backround)}}
         [search-area]
         [c/floating-button
          {:on-press #(dispatch [:search-patients])
           :title    "Next"
           :style    {:background-color
                      (if (:enable-next? @ui)
                        (s/colors :accent)
                        (s/colors :disabled))}}]]]])))
