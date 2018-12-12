(ns simple-experiments.view.new-patient
  (:require [clojure.string :as string]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [simple-experiments.events.simple-card :as simple-card]
            [simple-experiments.events.utils :as u]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]))

(defn input [field-name label-text props & {:keys [style]}]
  (let [show-errors? (subscribe [:ui-new-patient :show-errors?])
        error (subscribe [:ui-new-patient :errors field-name])]
    (r/create-class
     {:component-did-mount
      (fn []
        (when-not (string/blank? (:default-value props))
          (dispatch [:ui-new-patient field-name (str (:default-value props))])))
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

(defn expanding-input [ui field-name label-text props & {:keys [style]}]
  (if (or (= :none (:active-input ui))
            (= field-name (:active-input ui)))
    [input field-name label-text props :style style]))

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
  [expanding-input ui :age "Age"
   {:keyboard-type "numeric"
    :default-value (str
                    (or
                     (get-in ui [:values :age])
                     (:age ui-patient-search)))
    :max-length    4
    :style {:min-width "30%"}}])

(defn add-simple-card []
  [c/touchable-opacity
   {:style {:flex-direction "row"
            :align-items "center"
            :margin-top 8}
    :on-press #(dispatch [:goto :simple-card])}
   [c/micon {:name  "add-circle-outline"
             :color (s/colors :accent)
             :size  24
             :style {:margin-right 10}}]
   [c/text
    {:style {:font-size 14,
             :font-weight "500"
             :color (s/colors :accent)}}
    (string/upper-case "Add simple card")]])

(defn associated-active-card [active-card]
  [c/view {:flex-direction "row"
           :margin-bottom 2
           :background-color (s/colors :card-highlight)
           :padding-vertical 4
           :padding-horizontal 8}
   [c/image
    {:source c/qr-scan-icon-gray
     :style {:width 24
             :height 24
             :margin-right 12}}]
   [c/text
    {:style {:font-size    16
             :color        (s/colors :primary-text)
             :margin-right 10
             :letter-spacing 1.5}}
    (:six-digit-display active-card)]])

(defn simple-cards []
  (let [active-card (subscribe [:active-card])]
    (fn []
      [c/view
       {:style {:margin-vertical 24}}
       [c/text
        {:style {:color (s/colors :placeholder)
                 :font-size 12
                 :margin-bottom 8}}
        "Simple cards"]
       (if (simple-card/pending-registration? @active-card)
         [associated-active-card @active-card]
         [add-simple-card])])))

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
        {:default-value (u/title-case
                         (or
                          (get-in @ui [:values :full-name])
                          (:full-name @ui-patient-search)))}]
       (if (= @setting "age")
         [age-input @ui @ui-patient-search]
         [c/view {:flex-direction "row"
                  :align-items    "flex-start"
                  :flex           1}
          [age-input @ui @ui-patient-search]
          (when (= :none (:active-input @ui))
            [c/text
             {:style {:font-size         16
                      :margin-top        30
                      :margin-horizontal 10}}
             "OR"])
          [expanding-input @ui :date-of-birth "Date of birth"
           {:default-value (or
                            (get-in @ui [:values :date-of-birth])
                            (:date-of-birth @ui-patient-search))}
           :style {:min-width "50%"}]])
       [simple-cards]
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
         :none-text     "No colony"
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
