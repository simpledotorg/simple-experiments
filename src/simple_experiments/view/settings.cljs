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
                         :margin-bottom 40}}
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

(defn coach-marks []
  (let [times-to-show (subscribe [:store-coach :times-to-show])]
    [c/view
     {:style {:flex-direction "row"
              :align-items    "center"
              :justify-content "space-between"}}
     [c/text
      {:style {:font-size    20
               :margin-right 20}}
      "Coach Marks"]
     [c/text-input-layout
      {:style             {:width "60%"}
       :keyboard-type     "numeric"
       :default-value     (str (or @times-to-show 1))
       :on-change-text    #(dispatch [:set-times-to-show %])}
      "Times to show"]]))

(defn overdue []
  (let [selected-value (subscribe [:store-settings :overdue])]
    [c/view
     {:style {:flex-direction "row"
              :align-items    "center"
              :justify-content "space-between"}}
     [c/text
      {:style {:font-size    20
               :margin-right 20}}
      "Overdue"]
     [c/picker
      {:selected-value (or @selected-value :one-month-later)
       :on-value-change (fn [value] (dispatch [:set-setting :overdue value]))
       :style {:width "60%"}}
      [c/picker-item {:label "Empty" :value :empty}]
      [c/picker-item {:label "1 month later" :value :one-month-later}]
      [c/picker-item {:label "6 months later" :value :six-months-later}]]]))

(defn page []
  [c/scroll-view
   {:sticky-header-indices [0]}
   [c/header "Settings"]
   [section "Config / Toggles"
    [c/view
     [coach-marks]
     [overdue]]]
   [section "Seed Data"
    [c/view
     (for [pt (:patient-types db-seed/patients)]
       ^{:key (str (random-uuid))}
       [c/text (str (:name pt) ": " (count (:variants pt)))])
     [select-district-and-state]
     [c/action-button "delete-sweep" :regular "Reset to seed data" reset-seed-data 42]]]
   [c/view {:style {:margin-vertical 20}}]])
