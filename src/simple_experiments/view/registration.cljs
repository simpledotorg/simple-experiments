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

(defn header []
  [c/view
   {:style {:flex-direction  "row"
            :justify-content "space-between"
            :margin          10}}
   [c/touchable-opacity
    {:on-press #(dispatch [:go-back])}
    [c/micon {:name  "arrow-back"
              :size  28
              :color (s/colors :white)
              :style {:margin-right 16
                      :margin-top   2}}]]
   [c/touchable-opacity
    {:on-press #(dispatch [:goto :settings])
     :style    {:align-self "flex-end"}}
    [c/micon {:name "settings" :size 26 :color "white"}]]])

(defn with-banner [component]
  [c/view
   {:style {:flex             1
            :background-color (s/colors :pale-gray)}}
   [c/view
    {:style {:background-color (s/colors :primary)
             :height           "50%"}}
    [header]
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
   {:style {:top              "-20%"
            :background-color (s/colors :white)
            :border-radius    4
            :elevation        4
            :margin           24
            :padding          40
            :padding-vertical 30
            :height           "50%"
            :max-height       250
            :align-items      "center"
            :justify-content  "center"}}
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
        {:auto-focus        true
         :style             {:font-size          20
                             :font-weight        "500"
                             :width              "100%"
                             :padding-horizontal 2
                             :min-height         45
                             :text-align         "center"
                             :flex               1
                             :color              (s/colors :primary-text)}
         :on-change-text    #(dispatch [:set-registration-field field-name %])
         :on-submit-editing #(when-not (string/blank? @field-value)
                               (on-submit))
         :default-value     @field-value
         :blur-on-submit    false}
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
    [c/view
     {:style {:flex 1
              :width "100%"
              :align-items "center"}}
     [field-input
      :security-pin
      secure-entry-props
      #(dispatch [:goto :registration-4])]
     [c/text
      {:style {:font-size 14
               :text-align "center"
               :color (s/colors :light-text)}}
      "Remember your PIN, you'll need it later."]]]])

(defn pin-verification-page []
  (let [mismatch? (subscribe [:ui-registration :pin-mismatch?])]
    [with-banner
     [input-card
      "Enter security PIN again"
      [c/view
       {:style {:flex 1
                :width "100%"
                :align-items "center"}}
       [field-input
        :security-pin-verification
        secure-entry-props
        #(dispatch [:verify-security-pin])]
       (when @mismatch?
         [c/text
          {:style {:font-size 14
                   :color (s/colors :error)}}
          "PIN doesn't match the original PIN."])]]]))

(defn location-access-page []
  [c/view
   {:style {:flex 1}}
   [c/header "Location Access"]
   [c/image {:source    c/location-illustration
             :resize-mode "cover"
             :style       {:width (:width c/dimensions)
                           :height (:width c/dimensions)}}]
   [c/action-button "my-location"
    :regular
    "Allow location access"
    #(dispatch [:goto :select-clinic])
    48
    :style {:margin-horizontal "10%"
            :top -48}]
   [c/view
    {:style {:flex 1
             :align-items "center"
             :justify-content "center"
             :margin-bottom 50}}
    [c/text
     {:style {:font-size 20
              :color (s/colors :primary-text)}}
     "Find your clinic faster"]
    [c/text
     {:style {:font-size 14
              :color (s/colors :light-text)}}
     "Your location will help us find your clinic"]]])

(def clinics
  (repeat 10 {:name "CHC Nathana"
             :address "55 Mansingh Road, colony 3, Hoshiarpur, Bhatinda, Punjab"}))

(defn select-clinic []
  [c/scroll-view
   {:sticky-header-indices [0]}
   [c/header "Select your clinic"]
   (for [{:keys [name address]} clinics]
     ^{:key (str (random-uuid))}
     [c/touchable-opacity
      {:on-press #(do (dispatch [:goto :home])
                      (dispatch [:registration-done]))
       :style {:margin-horizontal   24
               :padding-vertical     16
               :border-bottom-width 1
               :border-bottom-color (s/colors :border)}}
      [c/text
       {:style {:font-size     16
                :color         (s/colors :primary-text)
                :margin-bottom 4}}
       name]
      [c/text
       {:style {:font-size 14
                :color     (s/colors :light-text)}}
       address]])])
