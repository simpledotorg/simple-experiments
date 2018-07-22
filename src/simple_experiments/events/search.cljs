(ns simple-experiments.events.search
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [simple-experiments.view.components :as c]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.db :as db :refer [app-db]]
            [simple-experiments.events.utils :refer [assoc-into-db]]))

(defn enable-next? [db]
  (let [full-name (get-in db [:ui :patient-search :full-name])
        age (get-in db [:ui :patient-search :age])]
    (and (not (string/blank? full-name))
         (not (string/blank? age)))))

(defn search-patients [{:keys [db]} _]
  (if (enable-next? db)
    (let [full-name (get-in db [:ui :patient-search :full-name])
          age (js/parseInt (get-in db [:ui :patient-search :age]))
          pattern (re-pattern (str "(?i)" (string/trim full-name)))]
      {:db (->> (get-in db [:store :patients])
                vals
                (filter #(<= (- age 5) (:age %) (+ 5 age)))
                (filter #(re-find pattern (:full-name %)))
                (assoc-in db [:ui :patient-search :results]))
       :dispatch [:goto-select-mode]})
    {}))

(defn handle-patient-search [db [_ k v]]
  (let [x (assoc-in db [:ui :patient-search k] v)]
    (assoc-in x [:ui :patient-search :enable-next?] (enable-next? x))))

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

(defn register-events []
  (reg-event-db :ui-patient-search handle-patient-search)
  (reg-event-db :patient-search-clear clear)
  (reg-event-fx :search-patients search-patients)
  (reg-event-db :goto-select-mode goto-select-mode)
  (reg-event-db :goto-search-mode goto-search-mode))
