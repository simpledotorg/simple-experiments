(ns simple-experiments.view.common
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [simple-experiments.view.styles :as s]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [simple-experiments.view.components :as c]
            [simple-experiments.events.simple-card :as simple-card]))

(defn add-to-patient-header []
  (let [active-card (subscribe [:active-card])]
    (fn []
      (when (simple-card/pending? @active-card)
        [c/header
         [c/text "Add "
          [c/text
           {:style {:letter-spacing 2}}
           (:six-digit-display @active-card)]
          " to patient"]]))))
