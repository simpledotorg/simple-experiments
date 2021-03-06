(ns simple-experiments.events.navigation
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [simple-experiments.view.components :as c]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.db :as db :refer [app-db]]))

(defonce screen-stack
  (atom [:home]))

(defn previous-screen []
  (-> @screen-stack pop last))

(defn special-cases [path]
  (cond
    (contains? #{[:home :simple-card :patient-summary]
                 [:home :simple-card :patient-search :patient-list :new-patient :patient-summary]
                 [:home :patient-search :patient-list :new-patient :patient-summary]}
               path)
    (fn [_] [:home])

    (contains? #{[:home :patient-search :patient-list :patient-summary]
                 [:home :simple-card :patient-list :patient-summary]
                 [:home :simple-card :patient-search :patient-list :patient-summary]}
               path)
    (fn [db]
      (if (= :associated (get-in db [:ui :active-card :status]))
        [:home]
        (pop path)))))

(defn go-back [db _]
  (cond
    (or (= @screen-stack [:home])
        (= @screen-stack [:registration]))
    (do (.exitApp c/back-handler)
        db)

    (special-cases @screen-stack)
    (let [path @screen-stack
          new-path-fn (special-cases @screen-stack)
          new-path (new-path-fn db)]
      (reset! screen-stack new-path)
      (assoc db :active-page (last new-path)))

    :else
    (do
      (swap! screen-stack pop)
      (assoc db :active-page (last @screen-stack)))))

(defn goto [db [_ page]]
  (swap! screen-stack conj page)
  (assoc db :active-page (last @screen-stack)))

(defn reset-to-home [db _]
  (reset! screen-stack [:home])
  (assoc db :active-page :home))

(defn reset-screen-stack [stack]
  (reset! screen-stack stack))

(defn register-events []
  (reg-event-db :goto goto)
  (reg-event-db :reset-to-home reset-to-home)
  (reg-event-db :go-back go-back))
