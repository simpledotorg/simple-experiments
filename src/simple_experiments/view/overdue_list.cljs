(ns simple-experiments.view.overdue-list
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.events.utils :as u]))

(defn patient-details [{:keys [full-name birth-year gender phone-number] :as patient}]
  (let [latest-bp (u/latest-bp patient)]
    [c/view
     {:style {:flex 1}}
     [c/view {:flex-direction "row"
              :align-items    "center"}
      [c/text
       {:style {:font-size 18
                :color     (s/colors :primary-text)}}
       full-name]
      [c/text
       {:style {:margin-left 10
                :font-size   16
                :color       (s/colors :light-text)}}
       (str "(" (string/capitalize gender) ", " (u/age birth-year) ")")]]
     [c/text
      {:style {:margin-top 4
               :font-size  16
               :color      (s/colors :light-text)}}
      (str (u/days-ago-text (u/last-visit patient)) ": "
           (:systolic latest-bp) "/" (:diastolic latest-bp))]
     [c/view
      {:style {:flex-direction "row"
               :align-items    "flex-end"}}
      [c/text
       {:style {:margin-top 4
                :font-size  16
                :color      (s/colors :overdue)}}
       (str (:overdue-days patient) " days overdue")]
      (when (:called-at patient)
        [c/view {:style {:flex-direction "row"
                         :align-items "center"
                         :margin-left 10}}
         [c/micon {:name "call-made"
                   :size     16
                   :color    (s/colors :called)}]
         [c/text
          {:style {:font-size 16
                   :color     (s/colors :called)
                   :margin-left 2}}
          (str "Called "
               (string/lower-case
                (u/days-ago-text
                 (time/in-days (time/interval (timec/from-long (:called-at patient)) (time/now))))))]])]]))

(defn call-card [patient]
  [c/view
   {:style {:flex             1
            :flex-direction   "row"
            :height           100
            :elevation        2
            :margin-vertical  5
            :padding          10
            :border-radius    4
            :border-width     1
            :border-color     (s/colors :light-border)
            :justify-content  "space-between"
            :background-color (s/colors :white)}}
   [patient-details patient]
   [c/view
    {:style {:border-left-width 1
             :border-left-color (s/colors :border)
             :justify-content "center"}}
    [c/touchable-opacity
     {:on-press #(dispatch [:make-call patient])
      :style {:padding-horizontal 12}}
     [c/micon {:name  "call"
               :size  28
               :color (s/colors :primary-text)}]]]])

(defn chip [text active? on-press]
  [c/touchable-opacity
   {:on-press on-press
    :style    {:margin-horizontal  5
               :border-radius      15
               :background-color   (if active?
                                     (s/colors :accent)
                                     (s/colors :pale-gray))
               :padding-horizontal 10
               :padding-vertical 4}}
   [c/text
    {:style {:font-size 16
             :color     (if active?
                          (s/colors :white)
                          (s/colors :accent))
             :min-width 30
             :text-align "center"}}
    text]])

(defn filters []
  (let [filter-by (subscribe [:ui-overdue-list :filter-by])]
    [c/view
     {:style {:flex-direction "row"
              :margin-bottom  20
              :align-items    "center"}}
     [c/text
      {:style {:font-size 16}}
      (string/upper-case "Overdue by: ")]
     [chip "All" (= @filter-by :all)
      #(dispatch [:set-overdue-filter :all])]
     [chip "1 to 10 days" (= @filter-by :one-to-ten)
      #(dispatch [:set-overdue-filter :one-to-ten])]]))

(defn content []
  (let [patients (subscribe [:overdue-patients])]
    (fn []
      [c/scroll-view
       {:content-container-style {:margin 16}}
       [filters]
       [c/view {:style {:flex 1
                        :margin-bottom 50}}
        (for [patient @patients]
          ^{:key (str (random-uuid))}
          [call-card patient])]])))
