(ns simple-experiments.view.registration
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.coach :as coach]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.events.utils :as u]))

(defn with-banner [component]
  [c/view
   {:style {:flex             1
            :background-color (s/colors :pale-gray)}}
   [c/view
    {:style {:background-color (s/colors :primary)
             :height           "60%"}}
    [c/touchable-opacity
     {:on-press #(dispatch [:goto :settings])
      :style    {:align-self "flex-end"
                 :margin     10}}
     [c/micon {:name "settings" :size 26 :color "white"}]]
    [c/view
     {:style {:flex-direction  "row"
              :align-items     "center"
              :justify-content "center"
              :flex            1
              :margin-bottom   "30%"}}
     [c/miconx {:name  "heart"
                :size  34
                :style {:margin-right 5}
                :color (s/colors :white)}]
     [c/text {:style {:font-size   34
                      :font-weight "bold"
                      :color       (s/colors :white)}}
      "Simple"]]]
   component])

(defn input-card [title input-field-component]
  [c/view
   {:style {:top                "-20%"
            :background-color   (s/colors :white)
            :border-radius      4
            :elevation          4
            :margin             24
            :padding-vertical   38
            :padding-horizontal 50
            :height             "50%"
            :max-height         200
            :align-items        "center"
            :justify-content    "center"}}
   [c/text
    {:style {:color         (s/colors :primary-text)
             :font-size     20
             :margin-bottom 16}}
    title]
   input-field-component])

(defn field-input [field-name props on-submit]
  (let [field-value (subscribe [:ui-registration field-name])]
    (fn []
      [c/text-input
       (merge-with
        merge
        {:auto-focus true
         :style      {:font-size   20
                      :font-weight "500"
                      :width       "100%"
                      :padding-horizontal  2
                      :min-height  40
                      :text-align  "center"
                      :flex        1
                      :color       (s/colors :primary-text)}
         :on-change-text    #(dispatch [:set-registration-field field-name %])
         :on-submit-editing #(when-not (string/blank? @field-value)
                               (on-submit))
         :default-value     @field-value}
        props)])))

(defn phone-number-entry []
  (let [phone-number (subscribe [:ui-registration :phone-number])]
    (fn []
      [c/view {:flex-direction  "row"
               :justify-content "center"
               :align-items     "center"}
       [c/text
        {:style {:font-size           20
                 :font-weight         "500"
                 :padding-bottom      8
                 :padding-horizontal  2
                 :color               (s/colors :primary-text)
                 :border-bottom-width 1
                 :border-color        (s/colors :primary-text)}}
        "+91"]
       [field-input
        :phone-number
        {:keyboard-type                 "numeric"
         :max-length        10
         :style             {:text-align "left"}}
        #(dispatch [:goto :registration-2])]])))

(defn phone-number-page []
  [with-banner
   [input-card
    "Your phone number"
    [phone-number-entry]]])

(defn full-name-page []
  (let [full-name (subscribe [:ui-registration :full-name])]
    (fn []
      [with-banner
       [input-card
        "Tell us your full name"
        [field-input
         :full-name
         {}
         #(dispatch [:goto :registration-3])]]])))

(def secure-entry-props
  {:keyboard-type "numeric"
   :placeholder "••••"
   :max-length 4
   :secure-text-entry true
   :style {:width "40%"
           :height 10
           :font-size 24
           :line-height 10}})

(defn pin-entry-page []
  [with-banner
   [input-card
    "Create your security PIN"
    [field-input
     :security-pin
     secure-entry-props
     #(dispatch [:goto :registration-4])]]])

(defn pin-verification-page []
  [with-banner
   [input-card
    "Enter security PIN again"
    [field-input
     :security-pin-verification
     secure-entry-props
     #(do (dispatch [:goto :home])
          (dispatch [:registration-done]))]]])
