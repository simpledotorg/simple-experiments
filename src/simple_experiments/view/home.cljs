(ns simple-experiments.view.home
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.view.overdue-list :as overdue-list]))

(defn tab [title active-tab title-text]
  (let [active? (= active-tab title)
        active-style {:color (s/colors :white)
                      :opacity 1
                      :border-bottom-width 2
                      :border-color (s/colors :white)}]
    [c/touchable-opacity
     {:on-press #(dispatch [:set-active-tab title])}
     [c/text
      {:style (merge {:color (s/colors :inactive-text)
                      :opacity 0.6
                      :padding-horizontal 30
                      :padding-vertical 15
                      :font-size 16}
                     (if active? active-style {}))}
      (string/upper-case title-text)]]))

(defn tabs []
  (let [active-tab (subscribe [:home :active-tab])]
    (fn []
      [c/view {:style {:flex-direction "row"
                       :justify-content "space-between"
                       :margin-top 14}}
       [tab :patient @active-tab "Patient"]
       [tab :overdue-list @active-tab "Overdue"]
       [tab :reports @active-tab "Reports"]])))

(defn header []
  [c/view {:style {:background-color (s/colors :primary)}}
   [c/view
    {:style {:flex-direction "row"
             :padding 10
             :justify-content "space-between"}}
    [c/view {:style {:flex 1
                     :flex-direction "row"
                     :align-items "center"}}
     [c/miconx {:name "heart"
                :size 26
                :style {:margin-right 5}
                :color (s/colors :white)}]
     [c/text {:style {:font-size 24
                      :font-weight "bold"
                      :color (s/colors :white)}}
      "Simple"]]
    [c/touchable-opacity
     {:on-press #(dispatch [:goto :settings])}
     [c/micon {:name "settings" :size 30 :color "white"}]]]
   [tabs]])

(defn patient-screen []
  [c/view
   {:style {:flex 1
            :flex-direction "column"
            :padding-horizontal 20}}
   [c/search-bar]
   [c/view
    [c/action-button
     "qrcode-scan"
     :community
     "Scan patient's Aadhaar"
     #(dispatch [:goto :aadhaar])
     54]
    [c/image {:source c/scan-illustration
              :resize-mode "contain"
              :style {:width (:width c/dimensions)
                      :height 380}}]]])

(defn reports []
  [c/text
   {:style {:font-size 18
            :padding 40
            :text-align "center"}}
   "Insufficient data to generate reports. Check back here after a month."])

(defn active-tab-content []
  (let [active-tab (subscribe [:home :active-tab])]
    (fn []
      [c/view
       {:style {:flex 1
                :flex-direction "column"}}
       (case @active-tab
         :patient   [patient-screen]
         :overdue-list [overdue-list/content]
         :reports   [reports])])))

(defn page []
  [c/view {:style {:flex 1}}
   [c/status-bar {:background-color (s/colors :primary-dark)}]
   [header]
   [active-tab-content]])
