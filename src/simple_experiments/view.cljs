(ns simple-experiments.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [simple-experiments.view.components :as c]
            [simple-experiments.events]
            [simple-experiments.subs]))

(defn header []
  [c/view {:style {:flex 1
                   :flex-direction "row"
                   :margin 10
                   :justify-content "space-between"}}
   [c/view {:style {:flex 1
                    :flex-direction "row"}}
    [c/text {:style {:margin-right 5
                     :font-size 20}}
     "🖤"]
    [c/text {:style {:font-size 20}}
     "RedApp"]]
   [c/text {:style {:font-size 20}}
    "settings"]])

(defn app-root []
  #_[c/view
     [c/text {:style {:font-size 30 :font-weight "100" :margin-bottom 20 :text-align "center"}}
      "ka boom"]
     [c/image {:source c/logo-img
               :style  {:width 80 :height 80 :margin-bottom 30}}]
     [c/touchable-highlight {:style {:background-color "#999" :padding 10 :border-radius 5}
                             :on-press #(c/alert "HELLO!")}
      [c/text {:style {:color "white" :text-align "center" :font-weight "bold"}} "press me"]]]
  [header]
  )
