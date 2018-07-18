(ns simple-experiments.view.patient-list
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]))

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

(defn page []
  (let [patients (subscribe [:patients])
        patient-search-results (subscribe [:patient-search-results])]
    (fn []
      [c/scroll-view {:style {:flex-direction "column"
                              :flex 1
                              :padding-horizontal 20}}
       [c/search-bar {:auto-focus true
                      :on-change-text #(dispatch [:handle-search-patients %])}]
       [c/view {:style {:margin-top 20}}
        (for [patient (or @patient-search-results @patients)]
          ^{:key (str (random-uuid))}
          [c/touchable-opacity
           {:on-press #(do (dispatch [:set-active-patient patient])
                           (dispatch [:show-bp-sheet]))}
           [patient-row patient]])]])))
