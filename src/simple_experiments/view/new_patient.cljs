(ns simple-experiments.view.new-patient
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]))

(defn input [field-name label-text props]
  (let [show-errors? (subscribe [:ui-new-patient :show-errors?])
        error (subscribe [:ui-new-patient :errors field-name])]
    (r/create-class
     {:component-did-mount
      (fn []
        (when-not (string/blank? (:default-value props))
          (dispatch [:ui-new-patient field-name (:default-value props)])))
      :reagent-render
      (fn []
        [c/text-input-layout
         (merge {:on-focus          #()
                 :on-change-text    #(dispatch [:ui-new-patient field-name %])
                 :on-submit-editing #(dispatch [:register-new-patient])
                 :style             {:margin-vertical 7}
                 :error             (if @show-errors? @error nil)}
                props)
         label-text])})))

(defn radios [labels action]
  (let [show-errors? (subscribe [:ui-new-patient :show-errors?])
        error (subscribe [:ui-new-patient :errors :gender])]
    (fn [labels action]
      [c/view
       [c/view {:style {:flex-direction "row"
                        :flex            1
                        :justify-content "space-between"
                        :margin-top 20
                        :margin-bottom 10}}
        (for [{:keys [text value active?]} labels]
          ^{:key (str (random-uuid))}
          [c/touchable-opacity
           {:on-press #(action value)
            :style    {:flex-direction  "row"
                       :align-items     "center"}}
           [c/micon {:name  (if active?
                              "radio-button-checked"
                              "radio-button-unchecked")
                     :size  22
                     :color (if active?
                              (s/colors :accent)
                              (s/colors :placeholder))}]
           [c/text {:style {:font-size   16
                            :margin-left 5}}
            (string/capitalize text)]])]
       (when (and @show-errors? (some? @error))
         [c/input-error-byline @error])])))

(defn select-gender [current-gender]
  [radios
   [{:text    "male" :value "male"
     :active? (= "male" current-gender)}
    {:text    "female" :value "female"
     :active? (= "female" current-gender)}
    {:text    "transgender" :value "transgender"
     :active? (= "transgender" current-gender)}]
   #(dispatch [:ui-new-patient :gender %])])

(defn fields []
  (let [ui-patient-search (subscribe [:ui-patient-search])
        ui                (subscribe [:ui-new-patient])]
    (fn []
      [c/view
       {:style {:flex-direction "column"
                :flex           1}}
       [input :full-name "Patient's full name"
        {:default-value (:full-name @ui-patient-search)}]
       [input :age "Patient's age (guess if unsure)"
        {:keyboard-type "numeric" :default-value (:age @ui-patient-search)}]
       [input :phone-number "Phone number"
        {:keyboard-type "numeric" :auto-focus true}]
       [select-gender (get-in @ui [:values :gender])]
       [input :village-or-colony "Village or Colony" {}]
       [c/view
        {:style {:flex-direction "row" :margin-top 10}}
        [input :district "District" {:default-value "Bathinda"}]
        [input :state "State" {:default-value "Punjab"}]]])))

(defn register-button []
  (let [ui (subscribe [:ui-new-patient])]
    (fn []
      (let [valid? (:valid? @ui)]
        [c/floating-button
         {:on-press #(dispatch [:register-new-patient])
          :title    "Register as a new patient"
          :style    {:background-color
                     (if valid?
                       (s/colors :accent)
                       (s/colors :disabled))
                     :font-size 16}
          :icon     [c/micon {:name  "check"
                              :color (s/colors :white)
                              :size  26
                              :style {:margin-right 10}}]}]))))

(defn interstitial []
  (let [show? (subscribe [:ui-new-patient :show-interstitial?])]
    (fn []
      [c/modal {:animation-type "fade"
                :transparent false
                :visible (true? @show?)
                :on-request-close #()}
       [c/view
        { :style {:flex 1
                  :justify-content "center"
                  :align-items "center"
                  :background-color (s/colors :window-backround)}}
        [c/micon {:name "check-circle"
                  :size 256
                  :color (s/colors :green)}]
        [c/text
         {:style {:font-size 42
                  :font-weight "500"
                  :color (s/colors :green)}}
         "Saved!"]]])))

(defn page []
  [c/view
   {:style {:flex-direction  "column"
            :justify-content "space-between"
            :align-items     "stretch"
            :flex            1}}
   [c/scroll-view
    {:keyboard-should-persist-taps "handled"
     :end-fill-color                "white"
     :ref (fn [com] (dispatch [:set-new-patient-sv-ref com]))
     :content-container-style
     {:flex-direction     "row"
      :padding-horizontal 16
      :padding-top        20
      :align-items        "flex-start"
      :border-color       "transparent"
      :background-color   "white"}}
    [c/touchable-opacity
     {:on-press #(dispatch [:goto :patient-list])}
     [c/micon {:name  "arrow-back"
               :size  28
               :color (s/colors :disabled)
               :style {:margin-right 16
                       :margin-top   2}}]]
    [fields]]
   [register-button]
   [interstitial]])
