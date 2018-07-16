(ns simple-experiments.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.events]
            [simple-experiments.subs]))

(defn tab [title active? target]
  (let [active-style {:color (s/colors :white)
                      :opacity 1
                      :border-bottom-width 2
                      :border-color (s/colors :white)}]
    [c/touchable-highlight
     [c/text
      {:style (merge {:color (s/colors :off-white)
                      :opacity 0.6
                      :padding-horizontal 30
                      :padding-vertical 15
                      :font-size 16}
                     (if active? active-style {}))}
      (string/upper-case title)]]))

(defn tabs []
  [c/view {:style {:flex-direction "row" :justify-content "space-between"}}
   [tab "patient" true nil]
   [tab "call list" false nil]
   [tab "reports" false nil]])

(defn header []
  [c/view {:style {:background-color (s/colors :primary)}}
   [c/view
    {:style {:flex-direction "row"
             :padding 10
             :justify-content "space-between"}}
    [c/view {:style {:flex 1
                     :flex-direction "row"
                     :align-items "center"}}
     [c/miconx {:name "heart" :size 26 :style {:margin-right 5}}]
     [c/text {:style {:font-size 24
                      :font-weight "bold"}}
      "Simple"]]
    [c/micon {:name "settings" :size 30 :color "white"}]]
   [tabs]])

(defn app-root []
  [c/view
   [c/status-bar {:background-color (s/colors :primary-dark)}]
   [header]])
