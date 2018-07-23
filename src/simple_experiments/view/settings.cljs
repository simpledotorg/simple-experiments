(ns simple-experiments.view.settings
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.db.seed :as db-seed]))

(defn header []
  [c/view {:style {:background-color (s/colors :primary)}}
   [c/view
    {:style {:flex-direction  "row"
             :padding         10
             :justify-content "space-between"}}
    [c/text {:style {:font-size   24
                     :font-weight "bold"}}
     "Settings"]]])

(defn section [title component]
  [c/view
   {:style {:padding-horizontal 20
            :margin-bottom 20}}
   [c/text
    {:style {:font-size 20
             :font-weight "bold"
             :border-bottom-width 1
             :border-color (s/colors :border)
             :padding-bottom 5
             :margin-vertical 10}}
    (string/capitalize title)]
   component])

(defn reset-seed-data []
  #(c/alert
    "Reset to seed data"
    "Are you sure you want to reset to seed data?"
    [{:text "No"}
     {:text    "Reset"
      :onPress (fn [] (dispatch [:reset-to-seed-data]))}]
    {:cancelable false}))

(defn page []
  [c/view
   [header]
   [section "Info"
    [c/view
     [c/text (str "State: " (:state db-seed/data))]
     [c/text (str "District: " (:district db-seed/data))]]]
   [section "Seed Data"
    [c/view
     (for [pt (:patient-types db-seed/data)]
       ^{:key (str (random-uuid))}
       [c/text (str (:name pt) ": " (count (:variants pt)))])
     [c/action-button "delete-sweep" :regular "Reset to seed data" reset-seed-data 42]]]])
