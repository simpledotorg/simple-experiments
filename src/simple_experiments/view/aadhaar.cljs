(ns simple-experiments.view.aadhaar
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.events.utils :as u]))

(defn qr-scan []
  (r/create-class
   {:component-will-unmount #(dispatch [:hide-camera])
    :reagent-render
    (fn []
      [c/view {:style {:flex 1
                       :flex-direction "column"
                       :background-color (s/colors :black)}}
       [c/qrcode-scanner
        {:on-read (fn [e]
                    (dispatch [:parse-qr e])
                    (dispatch [:hide-camera]))
         :reactivate true
         :style {:justify-content "flex-end"
                 :align-items "center"
                 :height 100
                 :flex 1}}]])}))

(defn close-overlay []
  [c/touchable-opacity
   {:on-press #(dispatch [:go-back])
    :style {:position "absolute"
            :top -40
            :align-items "center"
            :justify-content "center"
            :border-radius 50
            :background-color (s/colors :disabled)
            :width 80
            :height 80}}
   [c/micon {:name  "close"
             :color (s/colors :white)
             :size 40}]])

(defn focus-overlay []
  [c/view
   {:style {:position "absolute"
            :width "100%"
            :height "100%"}}
   [c/view {:style {:height "20%"
                    :background-color (s/colors :overlay-light)}}]
   [c/view {:style {:height "60%"
                    :flex-direction "row"}}
    [c/view {:style {:width "15%"
                     :background-color (s/colors :overlay-light)}}]
    [c/view {:style {:width "70%"
                     :border-width 4
                     :border-color (s/colors :green)}}]
    [c/view {:style {:width "15%"
                     :background-color (s/colors :overlay-light)}}]]
   [c/view {:style {:height "20%"
                    :background-color (s/colors :overlay-light)}}]])

(defn page []
  [c/view
   {:style {:flex 1
            :margin-top 20}}
   [c/view
    {:style {:margin-horizontal 20}}
    [c/search-bar]]
   [c/view
    {:style {:flex 1
             :align-items "center"
             :margin-top 60}}
    [c/view
     {:style {:flex 1}}
     [qr-scan]]
    [focus-overlay]
    [close-overlay]]])
