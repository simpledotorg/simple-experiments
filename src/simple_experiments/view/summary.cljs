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
  [c/view
   {:style {:background-color (s/colors :primary)
            :flex-direction "column"
            :padding 40}}
   [c/text
    {:style {:color "white" :font-size 24 :font-weight "bold"}}
    (string/capitalize full-name)]
   [c/text
    {:style {:color "white" :font-size 18}}
    (str (string/capitalize gender) ", " age " • " phone-number)]
   [c/text
    {:style {:color "white" :font-size 18}}
    (str street-name ", " village-or-colony)]])

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
      (let [{:keys [blood-pressures]} @active-patient]
        [c/scroll-view
         [summary-header @active-patient]
         [bp-list blood-pressures]]))))
