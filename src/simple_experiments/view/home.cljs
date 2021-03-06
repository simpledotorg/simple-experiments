(ns simple-experiments.view.home
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.view.coach :as coach]
            [simple-experiments.view.overdue-list :as overdue-list]))

(defn tab [title active-tab title-text & {:keys [badge]}]
  (let [active?      (= active-tab title)
        active-style {:opacity             1
                      :border-bottom-width 2
                      :border-color        (s/colors :white)}]
    [c/touchable-opacity
     {:on-press #(dispatch [:set-active-tab title])
      :style    (merge {:flex-direction     "row"
                        :padding-horizontal 20
                        :padding-vertical   15}
                       (if active? active-style {}))}
     [c/text
      {:style {:color (if active?
                        (s/colors :white)
                        (s/colors :inactive-text))
               :font-size 16}}
      (string/upper-case title-text)]
     (when (and (some? badge)
                (not= 0 badge))
       [c/text
        {:style {:align-self         "center"
                 :margin-left        8
                 :font-size          12
                 :color              (s/colors :white)
                 :padding-horizontal 6
                 :background-color   (s/colors :disabled)
                 :border-radius      4}}
        badge])]))

(defn tabs []
  (let [active-tab (subscribe [:home :active-tab])
        num-overdue-patients (subscribe [:num-overdue-patients])]
    (fn []
      [c/view {:style {:flex-direction "row"
                       :justify-content "space-between"
                       :margin-top 10}}
       [tab :patient @active-tab "Patient"]
       [tab :overdue-list @active-tab "Overdue" :badge @num-overdue-patients]
       [tab :reports @active-tab "Reports"]])))

(defn header []
  [c/view {:style {:background-color (s/colors :primary)}}
   [c/view
    {:style {:flex-direction  "row"
             :padding         10
             :align-items     "center"
             :justify-content "space-between"}}
    [c/image {:source      c/logo
              :resize-mode "contain"
              :style       {:width (* 0.35 (:width c/dimensions))
                            :height "100%"}}]
    [c/touchable-opacity
     {:on-press #(dispatch [:goto :settings])}
     [c/micon {:name "settings" :size 26 :color "white"}]]]
   [tabs]])

(defn illustration []
  [c/image {:source      c/scan-illustration
            :resize-mode "contain"
            :style       {:width  (* .90 (:width c/dimensions))
                          :height (* .75 (:width c/dimensions))}}])

(defn patient-screen []
  (let [ui-coach (subscribe [:ui-coach])]
    (fn []
      [c/view
       {:style {:flex               1
                :flex-direction     "column"
                :padding-horizontal 20}}
       [c/search-bar
        :style {:elevation (if (:search @ui-coach) 11 2)}]
       [c/view
        [c/view
         {:ref       #(dispatch [:set-ref :simple-card-button %])
          :on-layout #(dispatch [:measure :simple-card-button])
          :style     {:elevation (if (:scan @ui-coach) 11 2)}}
         [c/action-button-with-image
          c/qr-scan-icon-white
          "Scan Simple card"
          #(dispatch [:goto :simple-card])
          54]]
        [illustration]
        [c/text
         {:style {:font-size 16
                  :font-weight "normal",
                  :font-style "normal",
                  :line-height 24,
                  :letter-spacing 0.15,
                  :text-align "center",
                  :color (s/colors :light-text)}}
         "Record "
         [c/text
          {:style {:font-style "italic"
                   :color (s/colors :primary-text)}}
          "every patient"]
         " who has hypertension or who is taking medicines for high BP"]]])))

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

(defn coach-marks []
  (let [ui-coach        (subscribe [:ui-coach])
        ui-measurements (subscribe [:ui-measurements])
        active-tab      (subscribe [:home :active-tab])]
    (fn []
      (cond
        (and (:search @ui-coach)
             (= :patient @active-tab))
        [coach/search
         {:top       (get-in @ui-measurements [:search-bar :bottom])
          :max-width "90%"}]

        (and (:scan @ui-coach)
             (= :patient @active-tab))
        [coach/scan
         {:top       (get-in @ui-measurements [:simple-card-button :bottom])
          :max-width "75%"}]

        (and (:overdue @ui-coach)
             (= :overdue-list @active-tab))
        [coach/overdue
         {:width "80%"
          :top   (min (get-in @ui-measurements [:expanded-overdue-card :bottom])
                      (* 0.65 (:height c/dimensions)))}]

        (and (:call @ui-coach)
             (= :overdue-list @active-tab))
        [coach/call
         {:width "80%"
          :top   (min (get-in @ui-measurements [:expanded-overdue-card :py])
                      (* 0.65 (:height c/dimensions)))}]

        (and (:patient-status @ui-coach)
             (= :overdue-list @active-tab))
        [coach/patient-status
         {:width "80%"
          :top   (min (get-in @ui-measurements [:expanded-overdue-card :bottom])
                      (* 0.65 (:height c/dimensions)))}]

        :else
        nil))))

(defn approval-requested []
  [c/view
   {:style {:background-color (s/colors :black)
            :padding          16
            :height           90
            :bottom           0}}
   [c/view {:style {:flex-direction   "row"}}
    [c/micon {:name  "access-time"
              :size  24
              :color (s/colors :white)
              :style {:margin-right 12}}]
    [c/view
     {:style {:flex 1}}
     [c/text
      {:style {:color     (s/colors :white)
               :font-size 16}}
      "Waiting for approval"]
     [c/text
      {:style {:color     (s/colors :off-white)
               :font-size 14}}
      "A supervisor will call you to verify your identity."]]]])

(defn approval-granted []
  [c/view
   {:style {:background-color (s/colors :black)
            :padding          16
            :height           90
            :bottom           0}}
   [c/view {:style {:flex-direction  "row"
                    :justify-content "center"
                    :align-items     "center"}}
    [c/view
     {:style {:flex         1
              :margin-right 26}}
     [c/text
      {:style {:color     (s/colors :white)
               :font-size 16}}
      "You've been approved!"]
     [c/text
      {:style {:color     (s/colors :off-white)
               :font-size 14}}
      "You can now see all patient data for your clinic."]]
    [c/touchable-opacity
     {:on-press #(dispatch [:set-setting :approval-status :none])}
     [c/text
      {:style {:color (s/colors :bright-green)}}
      "GOT IT"]]]])

(defn approval-banner []
  (let [approval-status (subscribe [:store-settings :approval-status])]
    (fn []
      (cond
        (= @approval-status "requested")
        [approval-requested]

        (= @approval-status "granted")
        [approval-granted]

        :else
        nil))))

(defn page []
  (let [active-tab (subscribe [:home :active-tab])]
    (fn []
      [c/view {:style {:flex 1}}
       [header]
       [active-tab-content]
       [coach-marks]
       (when (= :patient @active-tab)
         [approval-banner])])))
