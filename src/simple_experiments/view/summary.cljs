(ns simple-experiments.view.summary
  (:require
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [cljs-time.core :as time]
   [cljs-time.coerce :as timec]
   [clojure.string :as string]
   [simple-experiments.view.bp :as bp]
   [simple-experiments.view.components :as c]
   [simple-experiments.view.styles :as s]))

(defn summary-header [{:keys [full-name age gender street-name
                              village-or-colony phone-number]}]
  [c/view {:style {:flex-direction "row"
                   :background-color (s/colors :primary)
                   :padding-horizontal 16
                   :padding-vertical 20
                   :align-items "flex-start"
                   :elevation 10}}
   [c/touchable-opacity
    {:on-press #(dispatch [:goto :patient-list])}
    [c/micon {:name "arrow-back"
              :size 28
              :color (s/colors :white)
              :style {:margin-right 16
                      :margin-top 2}}]]
   [c/view
    {:style {:flex-direction "column"}}
    [c/text
     {:style {:color "white" :font-size 24 :font-weight "bold"}}
     (string/capitalize full-name)]
    [c/text
     {:style {:color "white" :font-size 16}}
     (str (string/capitalize gender) ", " age " • " phone-number)]
    [c/text
     {:style {:color "white" :font-size 16}}
     (str street-name ", " village-or-colony)]]])


(defn drug-row [{:keys [drug-name drug-dosage]}]
  [c/view {:style {:flex-direction   "row"
                   :justify-content  "flex-start"
                   :padding-vertical 8}}
   [c/text
    {:style {:font-size    20
             :font-weight  "bold"
             :margin-right 10
             :width        80
             :color        (s/colors :primary-text)}}
    drug-dosage]
   [c/text
    {:style {:font-size 20
             :color     (s/colors :primary-text-2)}}
    drug-name]])

(defn drugs-updated-since [drugs]
  (let [latest-drug-time (last (sort (map :updated-at drugs)))
        updated-days-ago (c/number-of-days-since (timec/from-long latest-drug-time))]
    [c/view {:style {:flex-direction  "column"
                     :justify-content "center"
                     :align-items     "flex-end"}}
     [c/text {:style {:font-size 16}} "Updated"]
     [c/text {:style {:font-size 18}} (str updated-days-ago " days ago")]]))

(defn drugs-list [drugs]
  [c/view {:style {:flex-direction  "row"
                   :justify-content "space-between"}}
   [c/view {:style {:flex-direction "column"}}
    (for [drug-details (map :drug-details drugs)]
      ^{:key (str (random-uuid))}
      [drug-row drug-details])]
   (when (seq drugs)
     [drugs-updated-since drugs])])

(defn prescription [drugs]
  [c/view {:style {:padding 32}}
   [drugs-list drugs]
   [c/action-button
    "local-pharmacy"
    :regular
    (if (not-empty drugs) "Update Medicines" "Add Medicines")
    #(c/alert "Feature unavailable.")]])

(defn page []
  (let [active-patient (subscribe [:active-patient])]
    (fn []
      [c/scroll-view
       [summary-header @active-patient]
       [c/view
        [prescription (:prescription-drugs @active-patient)]
        [c/view {:elevation 2
                 :height 1
                 :border-bottom 1
                 :border-bottom-color "transparent"}]
        [bp/history (:blood-pressures @active-patient)]
        [bp/bp-sheet]]])))
