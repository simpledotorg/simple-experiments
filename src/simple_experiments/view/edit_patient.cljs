(ns simple-experiments.view.edit-patient
  (:require [clojure.string :as string]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]
            [simple-experiments.events.simple-card :as simple-card]
            [simple-experiments.events.utils :as u]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.view.patient-form :as f]))

(defn page []
  [c/view
   {:style {:flex-direction "column"
            :flex           1}}
   [c/scroll-view
    {:keyboard-should-persist-taps "handled"
     :end-fill-color               "white"
     :ref                          (fn [com] (dispatch [:set-patient-form-sv-ref com]))
     :content-container-style
     {:flex-direction     "row"
      :padding-horizontal 8
      :padding-top        16
      :align-items        "flex-start"
      :border-color       "transparent"
      :background-color   "white"}}
    [c/touchable-opacity
     {:on-press #(dispatch [:go-back])}
     [c/micon {:name  "arrow-back"
               :size  24
               :color (s/colors :disabled)
               :style {:margin-right 8
                       :margin-top   10}}]]
    [f/fields]]
   [f/register-button {:title "Edit patient"
                       :on-press #(dispatch [:update-patient])}]])
