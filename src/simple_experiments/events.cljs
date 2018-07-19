(ns simple-experiments.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.db :as db :refer [app-db]]))

(defn assoc-into-db [k]
  (fn [db [_ & args]]
    (let [ks (butlast args)
          value (last args)]
      (assoc-in db (cons k ks) value))))

(defn set-active-tab [db [_ active-tab]]
  (assoc-in db [:home :active-tab] active-tab))

(defn add-patient [{:keys [db]} [_ patient]]
  {:db (assoc-in db [:store :patients (:id patient)] patient)
   :dispatch [:persist-store]})

(defn goto [db [_ page]]
  (assoc db :active-page page))

(defn search-patients [db [_ search-query]]
  (let [pattern (re-pattern (str "(?i)" (string/trim search-query)))]
    (->> (get-in db [:store :patients])
         vals
         (filter #(re-find pattern (:full-name %)))
         (assoc db :patient-search-results))))

(defn handle-search-patients [_ [_ search-query]]
  {:dispatch-debounce
   [{:id ::search-patients-on-input-change
     :timeout 250
     :action :dispatch
     :event [:search-patients search-query]}]})

(defn set-active-patient-id [{:keys [db]} [_ patient-id]]
  {:db (assoc-in db [:ui :active-patient-id] patient-id)
   :dispatch [:goto :patient-summary]})

(defn handle-bp-keyboard [{:keys [db]} [_ kind value]]
  (cond
    (and (= kind :systolic)
         (or (and (re-find #"^[12]" value)
                  (= 3 (count value)))
             (and (not (re-find #"^[12]" value))
                  (= 2 (count value)))))
    (.focus (get-in db [:ui :bp :diastolic :ref])))
  {:db (assoc-in db [:ui :bp :value kind] value)})

(defn show-bp-sheet [db _]
  (assoc-in db [:ui :bp :visible?] true))

(defn hide-bp-sheet [db _]
  (assoc-in db [:ui :bp :visible?] false))

(defn show-custom-drug-sheet [db _]
  (assoc-in db [:ui :custom-drug :visible?] true))

(defn hide-custom-drug-sheet [db _]
  (assoc-in db [:ui :custom-drug :visible?] false))

(defn set-bp-ref [db [_ kind ref]]
  (assoc-in db [:ui :bp kind :ref] ref))

(defn timestamps []
  (let [ts (timec/to-long (time/now))]
    {:created-at ts
     :updated-at ts}))

(defn active-patient-id [db]
  (get-in db [:ui :active-patient-id]))

(defn fetch-new-bp [db]
  (merge
   {:systolic (get-in db [:ui :bp :value :systolic])
    :diastolic (get-in db [:ui :bp :value :diastolic])}
   (timestamps)))

(defn save-bp [{:keys [db]} _]
  {:db (update-in
        db
        [:store :patients (active-patient-id db) :blood-pressures]
        conj
        (fetch-new-bp db))
   :dispatch [:hide-bp-sheet]})

(defn remove-custom-drug [db [_ id]]
  (update-in
   db
   [:store :patients (active-patient-id db)
    :prescription-drugs :custom-drugs]
   dissoc
   id))

(defn save-drug [{:keys [db]} [_ id action]]
  (let [drug-name     (:drug-name (db-p/protocol-drugs-by-id id))
        other-drug-id (-> (map :id (drug-name (into {} db-p/protocol-drugs)))
                          set
                          (disj id)
                          first)
        path          [:store :patients (active-patient-id db)
                       :prescription-drugs :protocol-drugs :drug-ids]
        current-drugs (get-in db path)]
    {:db (-> db
             (update-in path (case action :add conj :remove disj) id)
             (update-in path disj other-drug-id))}))

(defn set-new-custom-drug [db [_ type value]]
  (assoc-in db [:ui :custom-drug type] value))

(defn fetch-new-custom-drug [db]
  (merge
   {:id (str (random-uuid))}
   (dissoc (get-in db [:ui :custom-drug]) :visible?)
   (timestamps)))

(defn save-custom-drug [{:keys [db]} _]
  (let [new-drug (fetch-new-custom-drug db)]
    {:db (assoc-in
          db
          [:store :patients (active-patient-id db)
           :prescription-drugs :custom-drugs (:id new-drug)]
          new-drug)
     :dispatch [:hide-custom-drug-sheet]}))

(defn register-events []
  (reg-event-db :initialize-db (fn [_ _] app-db))
  (reg-event-db :set-active-tab set-active-tab)
  (reg-event-fx :add-patient add-patient)
  (reg-event-db :goto goto)
  (reg-event-db :search-patients search-patients)
  (reg-event-fx :handle-search-patients handle-search-patients)
  (reg-event-fx :set-active-patient-id set-active-patient-id)
  (reg-event-db :show-bp-sheet show-bp-sheet)
  (reg-event-db :hide-bp-sheet hide-bp-sheet)
  (reg-event-db :show-custom-drug-sheet show-custom-drug-sheet)
  (reg-event-db :hide-custom-drug-sheet hide-custom-drug-sheet)
  (reg-event-fx :handle-bp-keyboard handle-bp-keyboard)
  (reg-event-db :set-bp-ref set-bp-ref)
  (reg-event-db :set-new-custom-drug set-new-custom-drug)
  (reg-event-fx :save-bp save-bp)
  (reg-event-fx :save-drug save-drug)
  (reg-event-db :remove-custom-drug remove-custom-drug)
  (reg-event-fx :save-custom-drug save-custom-drug))

(register-events)
