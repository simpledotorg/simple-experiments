(ns simple-experiments.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
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
      {:style (merge {:color (s/colors :white)
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

(defn search-bar [input-properties]
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
                   :margin-top         20}}
   [c/micon {:name  "search" :size 30
             :style {:margin-right 5}}]
   [c/text-input
    (merge {:placeholder             "Enter patient's name or phone"
            :placeholder-text-color  (s/colors :placeholder)
            :underline-color-android "transparent"
            :style                   {:flex      1
                                      :font-size 18}}
           input-properties)]])

(defn patient-screen []
  [c/view {:style {:flex-direction "column"
                   :padding-horizontal 20}}
   [search-bar {:on-focus #(dispatch [:goto :patient-list])}]
   [c/touchable-opacity
    {:on-press #(c/alert "ka boom!")
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

(defn home []
  [c/scroll-view {:style {:flex 1}}
   [c/status-bar {:background-color (s/colors :primary-dark)}]
   [header]
   [active-tab-content]])

(defn patient-row [{:keys [full-name gender age phone-number
                           street-name village-or-colony]} patient]
  [c/view {:style {:flex-direction "column"
                   :margin-vertical 10
                   :padding-bottom 10
                   :border-bottom-width 2
                   :border-bottom-color (s/colors :border)}}
   [c/view {:style {:flex-direction "row"
                    :justify-content "space-between"}}
    [c/text
     {:style {:color (s/colors :accent) :font-size 18}}
     (str (string/capitalize full-name) ", " (string/capitalize gender))]
    [c/view {:style {:flex-direction "row"}}
     [c/text {:style {:font-size 16}} "Age: "]
     [c/text {:style {:font-size 16
                      :font-weight "bold"}}
      age]]]
   [c/text
    {:style {:font-size 16}}
    (gstring/format "%s | %s, %s" phone-number street-name village-or-colony)]
   [c/text
    {:style {:font-size 14
             :color (s/colors :placeholder)}}
    (gstring/format "LAST VISIT: %s days ago" (rand-int 30))]])

(defn patient-list []
  (let [patients (subscribe [:patients])
        patient-search-results (subscribe [:patient-search-results])]
    (fn []
      [c/scroll-view {:style {:flex-direction "column"
                              :flex 1
                              :padding-horizontal 20}}
       [search-bar {:auto-focus true
                    :on-change-text #(dispatch [:handle-search-patients %])}]
       [c/view {:style {:margin-top 20}}
        (for [patient (or @patient-search-results @patients)]
          ^{:key (str (random-uuid))}
          [patient-row patient])]])))

(defn screen [display-name component on-back]
  (let [on-back (fn [] (on-back c/back-handler)
                  true)]
    (r/create-class
     {:display-name "home"
      :component-did-mount
      (fn [] (.addEventListener
              c/back-handler
              "hardwareBackPress"
              on-back))
      :component-will-unmount
      (fn [] (.removeEventListener
              c/back-handler
              "hardwareBackPress"
              on-back))
      :reagent-render component})))

(def pages
  {:home (screen "home" home #(.exitApp c/back-handler))
   :patient-list (screen "patient-list" patient-list #(dispatch [:goto :home]))})

(defn app-root []
  (let [active-page (subscribe [:active-page])]
    (fn []
      [(pages @active-page)])))
