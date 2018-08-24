(ns simple-experiments.view.new-patient
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]))

(defn input [field-name label-text props & {:keys [style]}]
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
                 :style             (merge {:margin-vertical 10} style)
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

(defn age-input [ui ui-patient-search]
  [input :age "Age"
   {:keyboard-type "numeric"
    :default-value (str
                    (or
                     (get-in ui [:values :age])
                     (:age ui-patient-search)))
    :max-length    4}])

(defn fields []
  (let [ui-patient-search (subscribe [:ui-patient-search])
        ui                (subscribe [:ui-new-patient])
        setting           (subscribe [:store-settings :age-vs-age-or-dob])
        seed              (subscribe [:seed])]
    (fn []
      [c/view
       {:style {:flex-direction "column"
                :flex           1}}
       [input :full-name "Patient's full name"
        {:default-value (or
                         (get-in @ui [:values :full-name])
                         (:full-name @ui-patient-search))}]
       (if (= @setting "age")
         [age-input @ui @ui-patient-search]
         [c/view {:flex-direction "row"
                  :align-items    "flex-start"
                  :flex           1}
          [input :date-of-birth "Date of birth (DD/MM/YYYY)"
           {:keyboard-type "numeric"
            :default-value (or
                            (get-in @ui [:values :date-of-birth])
                            (:date-of-birth @ui-patient-search))}
           :style {:width "50%"}]
          [c/text
           {:style {:font-size         16
                    :margin-top        30
                    :margin-horizontal 10}}
           "OR"]
          [c/view
           {:style {:width "26%"}}
           [age-input @ui @ui-patient-search]]])
       [input :phone-number "Phone number"
        {:keyboard-type "numeric"
         :auto-focus    true
         :allow-none?   true
         :none-text     "No phone"
         :on-none       #(dispatch [:ui-new-patient-none :phone-number %])}]
       [select-gender (get-in @ui [:values :gender])]
       [input :village-or-colony "Village or Colony"
        {:allow-none?   true
         :on-none       #(dispatch [:ui-new-patient-none :village-or-colony %])
         :default-value (get-in @ui [:values :village-or-colony])}]
       [c/view
        {:style {:flex-direction "row" :margin-top 10}}
        [input :district "District"
         {:default-value (or (get-in @ui [:values :district])
                             (:district @seed))}]
        [input :state "State"
         {:default-value (or (get-in @ui [:values :state])
                             (:state @seed))}]]])))

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

(defn page []
  [c/view
   {:style {:flex-direction "column"
            :flex           1}}
   [c/scroll-view
    {:keyboard-should-persist-taps "handled"
     :end-fill-color               "white"
     :ref                          (fn [com] (dispatch [:set-new-patient-sv-ref com]))
     :content-container-style
     {:flex-direction     "row"
      :padding-horizontal 8
      :padding-top        16
      :align-items        "flex-start"
      :border-color       "transparent"
      :background-color   "white"}}
    [c/touchable-opacity
     {:on-press #(dispatch [:go-back])}
     [c/micon {:name  "arrow-back"
               :size  24
               :color (s/colors :disabled)
               :style {:margin-right 8
                       :margin-top   10}}]]
    [fields]]
   [register-button]])
