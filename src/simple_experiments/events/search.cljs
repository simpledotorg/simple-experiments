(ns simple-experiments.events.search
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

(def all-validations
  {:full-name  [{:spec ::db-p/non-empty-string :error "Please enter patient's full name."}]
   :birth-year [{:spec ::db-p/non-empty-string :error "Please enter patient's birth year."}
                {:spec ::db-p/birth-year-string :error "Please enter a valid birth year."}]})

(defn errors [db]
  (let [full-name  (get-in db [:ui :patient-search :full-name])
        birth-year (get-in db [:ui :patient-search :birth-year])]
    {:full-name  (u/first-error (:full-name all-validations) full-name)
     :birth-year (u/first-error (:birth-year all-validations) birth-year)}))

(defn enable-next? [db]
  (every? nil? (vals (errors db))))

(defn search-patients [{:keys [db]} _]
  (if (enable-next? db)
    (let [full-name (get-in db [:ui :patient-search :full-name])
          birth-year (js/parseInt (get-in db [:ui :patient-search :birth-year]))
          pattern (re-pattern (str "(?i)" (string/trim full-name)))]
      {:db (->> (get-in db [:store :patients])
                vals
                (filter #(<= (- birth-year 5) (:birth-year %) (+ 5 birth-year)))
                (filter #(re-find pattern (:full-name %)))
                (assoc-in db [:ui :patient-search :results]))
       :dispatch-n [[:goto-select-mode]
                    [:set-search-coach-marks]]})
    {:db (assoc-in db [:ui :patient-search :show-errors?] true)}))

(defn handle-patient-search [db [_ k v]]
  (let [new-db (assoc-in db [:ui :patient-search k] v)
        new-errors (errors new-db)]
    (-> new-db
        (assoc-in [:ui :patient-search :enable-next?] (enable-next? new-db))
        (assoc-in [:ui :patient-search :errors] new-errors))))

(defn goto-select-mode [db _]
  (.dismiss c/keyboard)
  (assoc-in db [:ui :patient-search :mode] :select))

(defn goto-search-mode [db _]
  (-> db
      (assoc-in [:ui :patient-search :mode] :search)
      (assoc-in [:ui :patient-search :enable-next?] (enable-next? db))
      (assoc-in [:ui :patient-search :results] nil)))

(defn clear [db _]
  (assoc-in db [:ui :patient-search] nil))

(defn set-last-result-bottom [db [_ last-result-bottom]]
  (assoc-in db [:ui :patient-search :last-result-bottom]
            last-result-bottom))

(defn compute-last-result-bottom [db [_ last-result-bottom]]
  (let [com (get-in db [:ui :patient-search :last-result-ref])]
    (.measure com
              (fn [fx fy width height px py]
                (dispatch [:set-last-result-bottom (+ height py)])))
    db))

(defn set-last-result-ref [db [_ com]]
  (assoc-in db [:ui :patient-search :last-result-ref] com))

(defn register-events []
  (reg-event-db :set-last-result-ref set-last-result-ref)
  (reg-event-db :set-last-result-bottom set-last-result-bottom)
  (reg-event-db :compute-last-result-bottom compute-last-result-bottom)
  (reg-event-db :ui-patient-search handle-patient-search)
  (reg-event-db :patient-search-clear clear)
  (reg-event-fx :search-patients search-patients)
  (reg-event-db :goto-select-mode goto-select-mode)
  (reg-event-db :goto-search-mode goto-search-mode))
