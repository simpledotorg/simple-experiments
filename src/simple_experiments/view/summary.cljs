(ns simple-experiments.view.summary
  (:require
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [cljs-time.core :as time]
   [cljs-time.coerce :as timec]
   [clojure.string :as string]
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

(defn drugs-list [drugs]
  [c/view {:style {:flex-direction  "row"
                   :justify-content "space-between"}}
   [c/view {:style {:flex-direction "column"}}
    (for [drug-details (map :drug-details drugs)]
      ^{:key (str (random-uuid))}
      [drug-row drug-details])]
   (when (seq drugs)
     [c/view {:style {:flex-direction  "column"
                      :justify-content "center"
                      :align-items     "flex-end"}}
      [c/text {:style {:font-size 16}} "Updated"]
      [c/text {:style {:font-size 18}} "30 days ago"]])])

(defn prescription [drugs]
  [c/view {:style {:padding 32
                   :border-bottom-color "transparent"}}
   [drugs-list drugs]
   [c/action-button
    "local-pharmacy"
    :regular
    (if (not-empty drugs) "Update Medicines" "Add Medicines")
    #(c/alert "Feature unavailable.")]])

(defn bp-list [blood-pressures]
  [c/view
   {:style {:flex-direction "column"}}
   (for [{:keys [systolic diastolic created-at]}
         (sort-by :created-at > blood-pressures)]
     ^{:key (str (random-uuid))}
     [c/view {:style {:flex-direction "row"
                      :margin-vertical 10
                      :padding-bottom 10
                      :justify-content "space-between"
                      :margin-horizontal 20
                      :border-bottom-width 1
                      :border-bottom-color (s/colors :border)}}
      [c/miconx {:name "heart"
                 :color (rand-nth [(s/colors :high-bp)
                                   (s/colors :normal-bp)])
                 :size 30}]
      [c/text
       {:style {:font-size 24}}
       (str systolic "/" diastolic)]
      [c/text
       {:style {:font-size 24}}
       (str (time/in-days (time/interval (timec/from-long created-at)
                                         (time/now)))
            " days ago")]])])

(defn page []
  (let [active-patient (subscribe [:active-patient])]
    (fn []
      (let [{:keys [blood-pressures prescription-drugs]} @active-patient]
        [c/scroll-view
         [summary-header @active-patient]
         [c/view
          [prescription prescription-drugs]
          [c/view {:elevation 2
                   :height 1
                   :flex 1
                   :border-bottom 1
                   :border-bottom-color "transparent"}]
          [bp-list blood-pressures]]]))))
