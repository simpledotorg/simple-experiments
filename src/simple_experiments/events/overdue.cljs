(ns simple-experiments.events.overdue
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

(defn set-overdue-filter [db [_ filter-by]]
  (assoc-in db [:ui :overdue-list :filter-by] filter-by))

(defn mark-as-called [db [_ patient]]
  (assoc-in db [:store :patients (:id patient) :called-at]
            (timec/to-long (time/now))))

(defn make-call [{:keys [db]} [_ patient]]
  (let [link (str "tel:+91" (:phone-number patient))]
    (->(.canOpenURL c/linking link)
       (.then (fn [supported?]
                (if (not supported?)
                  (prn "cant call, sorry")
                  (do
                    (dispatch [:mark-as-called patient])
                    (.openURL c/linking link)))))))
  {})

(defn see-phone-number [db [_ patient]]
  (update-in db [:ui :overdue-list :see-phone-number? (:id patient)] not))

(defn expand-overdue-card [db [_ patient]]
  (update-in db [:ui :overdue-list :expand (:id patient)] not))

(defn show-skip-reason-sheet [db [_ patient]]
  (let [skip-reason (get-in db [:store :patients (:id patient) :skip-reason])]
    (-> db
        (assoc-in [:ui :overdue-list :skip-patient] patient)
        (assoc-in [:ui :overdue-list :show-skip-reason-sheet?] true)
        (assoc-in [:ui :overdue-list :skip-reason] skip-reason))))

(defn hide-skip-reason-sheet [db _]
  (-> db
      (assoc-in [:ui :overdue-list :show-skip-reason-sheet?] false)
      (assoc-in [:ui :overdue-list :skip-reason] nil)))

(defn set-skip-reason [db [_ patient reason]]
  (-> db
      (assoc-in [:ui :overdue-list :skip-reason] reason)
      (assoc-in [:store :patients (:id patient) :skip-reason] reason)))

(defn register-events []
  (reg-event-db :set-overdue-filter set-overdue-filter)
  (reg-event-db :expand-overdue-card expand-overdue-card)
  (reg-event-db :see-phone-number see-phone-number)
  (reg-event-fx :make-call make-call)
  (reg-event-db :mark-as-called mark-as-called)
  (reg-event-db :set-skip-reason set-skip-reason)
  (reg-event-db :show-skip-reason-sheet show-skip-reason-sheet)
  (reg-event-db :hide-skip-reason-sheet hide-skip-reason-sheet))
