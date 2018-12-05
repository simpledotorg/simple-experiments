(ns simple-experiments.view.simple-card
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.coach :as coach]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.events.utils :as u]))

(defn qr-scan []
  (r/create-class
   {:component-will-unmount #(dispatch [:hide-camera])
    :reagent-render
    (fn []
      [c/view
       [c/qrcode-scanner
        {:on-read (fn [e]
                    (dispatch [:handle-scan e])
                    (dispatch [:hide-camera]))
         :reactivate true
         :camera-style {:height "100%"}}]])}))

(defn focus-overlay []
  [c/view
   {:style {:position        "absolute"
            :width           "100%"
            :height          "100%"
            :flex            1
            :justify-content "center"
            :align-items     "center"}}
   [c/view {:style {:flex                1
                    :width               "100%"
                    :border-color        (s/colors :overlay-light)
                    :border-left-width   (* 0.15 (:width c/dimensions))
                    :border-right-width  (* 0.15 (:width c/dimensions))
                    :border-top-width    (* 0.20 (:height c/dimensions))
                    :border-bottom-width (* 0.20 (:height c/dimensions))}}
    [c/green-box {:style {:position "absolute" :top -10 :left -10}}     4 0 0 4]
    [c/green-box {:style {:position "absolute" :top -10 :right -10}}    4 4 0 0]
    [c/green-box {:style {:position "absolute" :bottom -10 :right -10}} 0 4 4 0]
    [c/green-box {:style {:position "absolute" :bottom -10 :left -10}}  0 0 4 4]]])

(defn simple-card []
  [c/view {:style {:flex-direction  "column"
                   :align-items     "center"
                   :padding         20}}
   [c/text {:style {:font-size      20
                    :font-weight    "500"
                    :font-style     "normal"
                    :line-height    28
                    :letter-spacing 0.15
                    :color          (s/colors :primary-text)}}
    "or type Simple card ID"]
   [c/text-input {:style {:width 80
                          :font-size 20
                          :margin-top 15}
                  :underline-color-android (s/colors :border)
                  :length 6
                  :keyboard-type "numeric"
                  :placeholder "123456"
                  :on-change-text #(dispatch [:handle-six-digit-keyboard %])
                  :on-submit-editing #(dispatch [:handle-six-digit-input])}]])

(defn page []
  (let [coach? (subscribe [:ui-coach :simple-card])]
    (r/create-class
     {:component-did-mount
      (fn []
        (dispatch [:set-coach-mark :simple-card]))
      :reagent-render
      (fn []
        [c/view
         {:style {:flex 1}}
         [c/header "Scan card ID"]
         [c/view
          {:style {:flex 1
                   :align-items "center"}}
          [c/view
           {:style {:flex 1}}
           [qr-scan]]
          [focus-overlay]
          [simple-card]]
         (when @coach?
           [coach/simple-card
            {:position "absolute"
             :width "80%"
             :bottom 20}])])})))
