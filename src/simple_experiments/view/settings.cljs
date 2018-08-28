(ns simple-experiments.view.settings
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.db.seed.data :as db-seed]))

(defn section
  ([title component]
   (section {} title component))
  ([props title component]
   [c/view
    (merge-with merge
                {:style {:padding-horizontal 20
                         :margin-bottom 20}}
                props)
    [c/text
     {:style {:font-size 20
              :font-weight "bold"
              :border-bottom-width 1
              :border-color (s/colors :border)
              :padding-bottom 5
              :margin-vertical 10}}
     (string/capitalize title)]
    component]))

(defn reset-seed-data []
  (c/alert
   "Reset to seed data"
   "Are you sure you want to reset to seed data?"
   [{:text "No"}
    {:text    "Reset"
     :onPress (fn [] (dispatch [:reset-to-seed-data]))}]
   {:cancelable false}))

(def available-locations
  [{:district "Bangalore" :state "Karnataka"}
   {:district "Bathinda" :state "Punjab"}])

(defn select-district-and-state []
  (let [seed (subscribe [:seed])]
    (fn []
      [c/view {:style {:flex-direction  "column"
                       :justify-content "flex-start"
                       :margin-top      20
                       :margin-bottom   10}}
       (doall
        (for [{:keys [state district]} available-locations
              :let                     [active? (= (:state @seed) state)]]
          ^{:key (str (random-uuid))}
          [c/touchable-opacity
           {:on-press #(dispatch [:set-seed-state-and-district state district])
            :style    {:flex-direction "row"
                       :align-items    "center"
                       :margin-bottom  10}}
           [c/micon {:name  (if active?
                              "radio-button-checked"
                              "radio-button-unchecked")
                     :size  22
                     :color (if active?
                              (s/colors :accent)
                              (s/colors :placeholder))}]
           [c/text {:style {:font-size   16
                            :margin-left 5}}
            (str district ", " state)]]))])))

(defn setting [subscription-vec label values on-setting-change & {:as opts}]
  (let [setting-value (subscribe subscription-vec)]
    [c/view
     {:style {:flex-direction "row"
              :align-items    "center"
              :justify-content "space-between"
              :flex 1}}
     [c/text
      {:style {:font-size    16
               :margin-right 20
               :width "40%"
               :flex-wrap "wrap"}}
      label]
     [c/picker
      {:selected-value (or @setting-value (:default-value opts))
       :on-value-change (fn [value] (on-setting-change value))
       :style {:width "50%"}
       :mode "dropdown"}
      (for [{:keys [label value]} values]
        ^{:key (str (random-uuid))}
        [c/picker-item {:label label :value value}])]]))

(defn toggles []
  [c/view
   [setting
    [:store-coach :times-to-show]
    "Show coach marks"
    [{:label "None" :value 0}
     {:label "Once" :value 1}
     {:label "Twice" :value 2}
     {:label "Thrice" :value 3}]
    (fn [value] (dispatch [:set-times-to-show value]))
    :default-value 1]

   [setting
    [:store-settings :overdue]
    "Overdue mode"
    [{:label "Empty" :value :empty}
     {:label "1 month later" :value :one-month-later}
     {:label "6 months later" :value :six-months-later}]
    (fn [value] (dispatch [:set-setting :overdue value]))
    :default-value :one-month-later]

   [setting
    [:store-settings :age-vs-age-or-dob]
    "Age vs Age or DoB"
    [{:label "Age" :value :age}
     {:label "Age or DoB" :value :age-or-dob}]
    (fn [value] (dispatch [:set-setting :age-vs-age-or-dob value]))
    :default-value :age]

   [setting
    [:store-settings :start-screen]
    "Start screen"
    [{:label "Home" :value :home}
     {:label "Registration" :value :registration}]
    (fn [value] (dispatch [:set-setting :start-screen value]))
    :default-value :home]

   [setting
    [:store-settings :approval-status]
    "Approval Status"
    [{:label "None" :value :none}
     {:label "Requested" :value :requested}
     {:label "Granted" :value :granted}]
    (fn [value] (dispatch [:set-setting :approval-status value]))
    :default-value :none]])

(defn page []
  [c/scroll-view
   {:sticky-header-indices [0]}
   [c/header "Settings"]
   [section "Config / Toggles"
    [toggles]]
   [section "Seed Data"
    [c/view
     [select-district-and-state]
     [c/action-button "delete-sweep" :regular "Reset to seed data" reset-seed-data 42]]]
   [c/view {:style {:margin-vertical 20}}]])
