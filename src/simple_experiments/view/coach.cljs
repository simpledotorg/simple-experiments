(ns simple-experiments.view.coach
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.events.utils :as u]))

(defn triangle
  ([size color]
   (triangle size color {}))
  ([size color props]
   [c/view
    (merge-with
     merge
     {:style
      {:width             0
       :height            0
       :background-color   "transparent"
       :border-style       "solid"
       :border-left-width   (* 2 size)
       :border-right-width  (* 2 size)
       :border-bottom-width (* 2 size)
       :border-left-color   "transparent"
       :border-right-color  "transparent"
       :border-bottom-color color}}
     props)]))

(defn overlay-sheet
  ([component]
   (overlay-sheet {} component))
  ([props component]
   [c/touchable-opacity
    (merge-with
     merge
     {:style {:position "absolute"
              :flex 1
              :width "100%"
              :height "100%"
              :background-color (s/colors :overlay-dark)
              :align-items "center"}}
     props)
    component]))

(defn dialogue-container [props component]
  [c/view
   (merge-with merge
               {:style {:align-items "center"
                        :margin-top 10}}
               props)
   [c/view
    {:style {:background-color (s/colors :dialogue-light)
             :border-radius 5
             :elevation 10
             :padding 20}}
    component]
   [triangle 30 (s/colors :dialogue-light)
    {:style {:position "absolute"
             :top -10}}]])


(defn dialogue-box
  ([title content]
   (dialogue-box {} title content))
  ([props title content]
   [dialogue-container
    props
    [c/view
     (when-not (string/blank? title)
       [c/text
        {:style {:font-weight "bold"
                 :font-size 18
                 :margin-bottom 10
                 :color (s/colors :primary-text)
                 :text-align "left"}}
        title])
     [c/text
      {:style {:font-size 17
               :color (s/colors :primary-text)}}
      content]]]))

(defn multiple-results [style num-results]
  [overlay-sheet
   {:on-press #(dispatch [:hide-coach-marks])}
   [dialogue-box
    {:style style}
    (str num-results " patients found with that name")
    "Ask patient for phone number, colony or last visit."]])

(defn single-result [style]
  [overlay-sheet
   {:on-press #(dispatch [:hide-coach-marks])}
   [dialogue-box
    {:style style}
    "1 patient found with that name"
    "If the patient's phone number or colony do not match, register as a new patient below."]])

(defn aadhaar [style]
  [overlay-sheet
   {:on-press #(do (dispatch [:hide-coach-marks])
                   (dispatch [:set-aadhaar-coach-mark]))}
   [dialogue-container
    {:style style}
    [c/view
     {:style {:flex-direction "row"
              :flex-wrap "nowrap"
              :justify-content "space-between"}}
     [c/text
      {:style {:font-size 16
               :color (s/colors :primary-text)
               :max-width "80%"}}
      "Scan code on the right hand side of the Aadhaar"]
     [c/miconx {:name "qrcode-scan"
                :size 36
                :color (s/colors :primary-text)}]]]])

(defn search-or-register [style]
  [overlay-sheet
   {:on-press #(dispatch [:hide-coach-marks])}
   [dialogue-box
    {:style style}
    nil
    "Register new patients or search for already registered patients here."]])

(defn new-blood-pressure [style]
  [overlay-sheet
   {:on-press #(dispatch [:hide-coach-marks])}
   [dialogue-box
    {:style style}
    nil
    "Blood pressure added."]])

(defn overdue [style]
  [overlay-sheet
   {:on-press #(dispatch [:hide-coach-marks])}
   [dialogue-box
    {:style style}
    nil
    "Call to remind patients who are overdue for follow up."]])
