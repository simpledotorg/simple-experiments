(ns simple-experiments.db
  (:require [clojure.spec.alpha :as s]))

;; initial state of app-db
(def app-db
  {:active-page :home
   :home {:active-tab :patient}})
