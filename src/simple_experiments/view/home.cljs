(ns simple-experiments.view.home
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]))

(defn tab [title active-tab target]
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
      (string/upper-case (name title))]]))

(defn tabs []
  (let [active-tab (subscribe [:home :active-tab])]
    (fn []
      [c/view {:style {:flex-direction "row" :justify-content "space-between"}}
       [tab :patient @active-tab nil]
       [tab :call-list @active-tab nil]
       [tab :reports @active-tab nil]])))

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

(defn patient-screen []
  [c/view {:style {:flex-direction "column"
                   :padding-horizontal 20}}
   [c/search-bar {:on-focus #(dispatch [:goto :patient-list])}]
   [c/action-button
    "qrcode-scan"
    :community
    "Scan patient's Aadhaar"
    #(c/alert "Feature unavailable.")]
   [c/image {:source c/scan-illustration
             :resize-mode "contain"
             :style {:width (:width c/dimensions)
                     :height 380}}]])

(defn call-list []
  [c/text
   {:style {:font-size 18
            :padding 40
            :text-align "center"}}
   "Insufficient data to generate call lists. Check back here after a month."])

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
         :call-list [call-list]
         :reports   [reports])])))

(defn page []
  [c/scroll-view {:style {:flex 1}}
   [c/status-bar {:background-color (s/colors :primary-dark)}]
   [header]
   [active-tab-content]])
