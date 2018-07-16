(ns simple-experiments.db
  (:require [simple-experiments.db.patient :as patient]))

;; initial state of app-db
(def app-db
  {:active-page :home
   :home {:active-tab :patient}
   :store {:patients []}})
