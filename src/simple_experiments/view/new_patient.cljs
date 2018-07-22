(ns simple-experiments.view.new-patient
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]))

(defn input [field-name label-text props]
  (r/create-class
   {:component-did-mount
    (fn []
      (when-not (string/blank? (:default-value props))
        (dispatch [:ui-new-patient :fields field-name (:default-value props)])))
    :reagent-render
    (fn []
      [c/text-input-layout
       (merge {:on-focus          #()
               :on-change-text    #(dispatch [:ui-new-patient :fields field-name %])
               :on-submit-editing #(dispatch [:register-new-patient])
               :style             {:margin-vertical 5}}
              props)
       label-text])}))

(defn radios [labels action]
  [c/view {:style {:flex-direction "row"
                   :flex            1
                   :justify-content "space-between"
                   :margin-vertical 20}}
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
       (string/capitalize text)]])])

(defn select-gender [current-gender]
  [radios
   [{:text    "male" :value "male"
     :active? (= "male" current-gender)}
    {:text    "female" :value "female"
     :active? (= "female" current-gender)}
    {:text    "transgender" :value "transgender"
     :active? (= "transgender" current-gender)}]
   #(dispatch [:ui-new-patient :fields :gender %])])

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
       [select-gender (get-in @ui [:fields :gender :value])]
       [input :village-or-colony "Village or Colony" {}]
       [input :district "District" {:default-value "Bathinda"}]
       [input :state "State" {:default-value "Punjab"}]])))

(defn register-button []
  (let [ui (subscribe [:ui-new-patient])]
    (fn []
      (let [valid? (:valid? @ui)]
        [c/view {:flex-direction  "row"
                 :justify-content "center"
                 :align-items     "center"
                 :background-color
                 (if valid?
                   (s/colors :accent)
                   (s/colors :disabled))}
         [c/micon {:name "check" :color (s/colors :white) :size 26}]
         [c/floating-button
          {:on-press #(when valid?
                        (dispatch [:register-new-patient]))
           :title    "Register as a new patient"
           :style    {:background-color "transparent"
                      :font-size        16
                      :margin-left      10}}]]))))

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
   [register-button]])
