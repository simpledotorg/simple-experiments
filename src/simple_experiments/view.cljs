(ns simple-experiments.view
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [reagent.core :as r]
            [clojure.string :as string]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.view.home :as home]
            [simple-experiments.view.patient-list :as patient-list]
            [simple-experiments.view.patient-search :as patient-search]
            [simple-experiments.view.summary :as patient-summary]
            [simple-experiments.view.prescription-drugs :as drugs]
            [simple-experiments.view.new-patient :as new-patient]
            [simple-experiments.view.settings :as settings]
            [simple-experiments.view.simple-card :as simple-card]
            [simple-experiments.view.registration :as registration]
            [simple-experiments.events]
            [simple-experiments.subs]))

(def pages
  {:home               (c/screen "home"
                                 home/page)
   :patient-list       (c/screen "patient-list"
                                 patient-list/page)
   :patient-search     (c/screen "patient-search"
                                 patient-search/page)
   :new-patient        (c/screen "new-patient"
                                 new-patient/page)
   :patient-summary    (c/screen "patient-summary"
                                 patient-summary/page)
   :prescription-drugs (c/screen "prescription-drugs"
                                 drugs/page)
   :settings           (c/screen "settings"
                                 settings/page)
   :simple-card        (c/screen "simple-card"
                                 simple-card/page)
   :registration       (c/screen "registration"
                                 registration/phone-number-page)
   :registration-2     (c/screen "registration"
                                 registration/full-name-page)
   :registration-3     (c/screen "registration"
                                 registration/pin-entry-page)
   :registration-4     (c/screen "registration"
                                 registration/pin-verification-page)
   :location-access    (c/screen "location-access"
                                 registration/location-access-page)
   :select-clinic      (c/screen "select-clinic"
                                 registration/select-clinic)})

(defn app-root []
  (let [active-page (subscribe [:active-page])]
    (fn []
      [(pages @active-page)])))
