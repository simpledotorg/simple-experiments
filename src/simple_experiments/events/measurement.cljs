(ns simple-experiments.events.measurement
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [simple-experiments.view.components :as c]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.db :as db :refer [app-db]]
            [simple-experiments.events.utils :as u :refer [assoc-into-db]]))

(defn set-measurements [db [_ component-name measurements]]
  (let [[fx fy width height px py] measurements]
    (update-in
     db [:ui :measurements component-name]
     merge
     {:fx     fx
      :fy     fy
      :width  width
      :height height
      :px     px
      :py     py
      :bottom (+ height py)})))

(defn measure [{:keys [db]} [_ component-name]]
  (let [component (get-in db [:ui :measurements component-name :ref])]
    (when component
      (.measure
       component
       (fn [& measurements]
         (dispatch [:set-measurements component-name measurements]))))
    {}))

(defn set-ref [db [_ component-name component]]
  (assoc-in db [:ui :measurements component-name :ref] component))

(defn register-events []
  (reg-event-db :set-measurements set-measurements)
  (reg-event-db :set-ref set-ref)
  (reg-event-fx :measure measure))
