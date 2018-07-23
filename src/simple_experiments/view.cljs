(ns simple-experiments.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.view.home :as home]
            [simple-experiments.view.patient-list :as patient-list]
            [simple-experiments.view.summary :as patient-summary]
            [simple-experiments.view.prescription-drugs :as drugs]
            [simple-experiments.view.new-patient :as new-patient]
            [simple-experiments.view.settings :as settings]
            [simple-experiments.events]
            [simple-experiments.subs]))

(def pages
  {:home               (c/screen "home"
                                 home/page
                                 #(.exitApp c/back-handler))
   :patient-list       (c/screen "patient-list"
                                 patient-list/page
                                 #(dispatch [:goto :home]))
   :new-patient        (c/screen "new-patient"
                                 new-patient/page
                                 #(dispatch [:goto :patient-list]))
   :patient-summary    (c/screen "patient-summary"
                                 patient-summary/page
                                 #(dispatch [:goto :patient-list]))
   :prescription-drugs (c/screen "prescription-drugs"
                                 drugs/page
                                 #(dispatch [:goto :patient-summary]))
   :settings           (c/screen "settings"
                                 settings/page
                                 #(dispatch [:goto :home]))})

(defn app-root []
  (let [active-page (subscribe [:active-page])]
    (fn []
      [(pages @active-page)])))
