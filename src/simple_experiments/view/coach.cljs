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

(defn dialogue-box
  ([title content]
   (dialogue-box {} title content))
  ([props title content]
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
     [c/text
      {:style {:font-weight "bold"
               :font-size 18
               :color (s/colors :primary-text)}}
      title]
     [c/text
      {:style {:font-size 17
               :margin-top 10
               :color (s/colors :primary-text)}}
      content]]
    [triangle 30 (s/colors :dialogue-light)
     {:style {:position "absolute"
              :top -10}}]]))

(defn multiple-results [style]
  [overlay-sheet
   {:on-press #(dispatch [:hide-search-coach-marks])}
   [dialogue-box
    {:style style}
    "2 patients found with that name"
    "Ask patient for phone number, colony or last visit."]])
