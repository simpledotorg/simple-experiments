(ns simple-experiments.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.view.home :as home]
            [simple-experiments.view.patient-list :as patient-list]
            [simple-experiments.events]
            [simple-experiments.subs]))

(def pages
  {:home         (c/screen "home"
                           home/page
                           #(.exitApp c/back-handler))
   :patient-list (c/screen "patient-list"
                           patient-list/page
                           #(dispatch [:goto :home]))})

(defn app-root []
  (let [active-page (subscribe [:active-page])]
    (fn []
      [(pages @active-page)])))
