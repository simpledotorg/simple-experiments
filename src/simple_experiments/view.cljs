(ns simple-experiments.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.events]
            [simple-experiments.subs]))

(defn tab [title active-tab target]
  (let [active? (= active-tab title)
        active-style {:color (s/colors :white)
                      :opacity 1
                      :border-bottom-width 2
                      :border-color (s/colors :white)}]
    [c/touchable-opacity
     {:on-press #(dispatch [:set-active-tab title])}
     [c/text
      {:style (merge {:color (s/colors :off-white)
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

(defn search-bar []
  [c/view {:style {:flex-direction     "row"
                   :align-items        "center"
                   :shadow-offset      {:width 10 :height 10}
                   :shadow-color       "black"
                   :shadow-opacity     1.0
                   :padding-horizontal 10
                   :padding-vertical   5
                   :border-width       1
                   :border-color       "transparent"
                   :elevation          1
                   :margin-top    20}}
   [c/micon {:name  "search" :size 30
             :style {:margin-right 5}}]
   [c/text-input {:placeholder             "Enter patient's name or phone"
                  :placeholder-text-color  (s/colors :placeholder)
                  :underline-color-android "transparent"
                  :style                   {:flex      1
                                            :font-size 18}}]])

(defn patient-screen []
  [c/view {:style {:flex-direction "column"
                   :padding-horizontal 20}}
   [search-bar]
   [c/touchable-opacity
    {:on-press #(c/alert "ka boom")
     :style {:margin-top 20
             :background-color (s/colors :accent)
             :border-radius 2
             :elevation 1
             :height 56
             :flex-direction "row"
             :align-items "center"
             :justify-content "center"}}
    [c/miconx {:name "qrcode-scan"
               :size 26
               :color (s/colors :white)
               :style {:margin-right 10}}]
    [c/text {:style {:color (s/colors :white)
                     :font-size 20}}
     "Scan patient's Aadhaar"]]
   [c/image {:source c/scan-illustration
             :resize-mode "contain"
             :style {:width (:width c/dimensions)
                     :height 380}}]])

(defn call-list []
  [c/text "call list"])

(defn reports []
  [c/text "reports"])

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

(defn app-root []
  [c/scroll-view {:style {:flex 1}}
   [c/status-bar {:background-color (s/colors :primary-dark)}]
   [header]
   [active-tab-content]])
