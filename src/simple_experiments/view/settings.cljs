(ns simple-experiments.view.settings
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.db.seed :as db-seed]))

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
      [c/view {:style {:flex-direction "row"
                       :justify-content "space-around"
                       :margin-top 20
                       :margin-bottom 10}}
       (doall
        (for [{:keys [state district]} available-locations
              :let [active? (= (:state @seed) state)]]
          ^{:key (str (random-uuid))}
          [c/touchable-opacity
           {:on-press #(dispatch [:set-seed-state-and-district state district])
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
            (str district ", " state)]]))])))

(defn coach-marks []
  (let [times-to-show (subscribe [:store-coach :times-to-show])]
    [section
     {:style {:flex 1}}
     "Coach Marks"
     [c/view
      {:style {:flex-direction "column"}}
      [c/text-input-layout
       {:keyboard-type     "numeric"
        :default-value     (str (or @times-to-show 1))
        :on-change-text    #(dispatch [:set-times-to-show %])
        :on-submit-editing #(dispatch [:save-times-to-show])}
       "Times to show"]
      [c/action-button-outline "check" :regular "Set times to show"
       #(dispatch [:set-times-to-show]) 42]]]))

(defn page []
  [c/scroll-view
   {:sticky-header-indices [0]}
   [c/header "Settings"]
   [section "Seed Data"
    [c/view
     (for [pt (:patient-types db-seed/data)]
       ^{:key (str (random-uuid))}
       [c/text (str (:name pt) ": " (count (:variants pt)))])
     [select-district-and-state]
     [c/action-button "delete-sweep" :regular "Reset to seed data" reset-seed-data 42]]]
   [coach-marks]])
