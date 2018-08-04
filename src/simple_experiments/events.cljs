(ns simple-experiments.events
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.db :as db :refer [app-db]]
            [simple-experiments.events.navigation :as nav]
            [simple-experiments.events.scan :as scan]
            [simple-experiments.events.search :as search]
            [simple-experiments.events.register :as register]
            [simple-experiments.events.overdue :as overdue]
            [simple-experiments.events.utils :as u :refer [assoc-into-db]]))

(defn set-active-tab [db [_ active-tab]]
  (assoc-in db [:home :active-tab] active-tab))

(defn add-patient [{:keys [db]} [_ patient]]
  {:db (assoc-in db [:store :patients (:id patient)] patient)
   :dispatch [:persist-store]})

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
  (assoc-in db [:ui :bp] {:visible? true}))

(defn hide-bp-sheet [db _]
  (assoc-in db [:ui :bp] {:visible? false}))

(defn show-custom-drug-sheet [db _]
  (assoc-in db [:ui :custom-drug] {:visible? true}))

(defn hide-custom-drug-sheet [db _]
  (assoc-in db [:ui :custom-drug] {:visible? false}))

(defn set-bp-ref [db [_ kind ref]]
  (assoc-in db [:ui :bp kind :ref] ref))

(defn timestamps []
  (let [ts (timec/to-long (time/now))]
    {:created-at ts
     :updated-at ts}))

(defn fetch-new-bp [db]
  (merge
   {:systolic (get-in db [:ui :bp :value :systolic])
    :diastolic (get-in db [:ui :bp :value :diastolic])}
   (timestamps)))

(defn save-bp [{:keys [db]} _]
  (let [new-bp (fetch-new-bp db)
        patient-id (u/active-patient-id db)]
    (if (and (not (string/blank? (:systolic new-bp)))
             (not (string/blank? (:diastolic new-bp))))
      {:db (-> db
               (update-in [:store :patients patient-id :blood-pressures]
                          conj (fetch-new-bp db))
               (assoc-in [:store :patients patient-id :next-visit]
                         nil))
       :dispatch-n [[:hide-bp-sheet]
                    [:persist-store]]}
      {:dispatch [:hide-bp-sheet]})))

(defn remove-custom-drug [{:keys [db]} [_ id]]
  {:db (update-in
        db
        [:store :patients (u/active-patient-id db)
         :prescription-drugs :custom-drugs]
        dissoc
        id)
   :dispatch [:persist-store]})

(defn save-drug [{:keys [db]} [_ id action]]
  (let [drug-name           (:drug-name (db-p/protocol-drugs-by-id id))
        other-drug-id       (-> (map :id (drug-name (into {} db-p/protocol-drugs)))
                                set
                                (disj id)
                                first)
        protocol-drugs-path [:store :patients (u/active-patient-id db)
                             :prescription-drugs :protocol-drugs]
        path                (conj protocol-drugs-path :drug-ids)
        current-drugs       (get-in db path)]
    {:db       (-> db
                   (update-in path (case action :add conj :remove disj) id)
                   (update-in path set)
                   (update-in path disj other-drug-id)
                   (assoc-in (conj protocol-drugs-path :updated-at)
                             (:updated-at (timestamps))))
     :dispatch [:persist-store]}))

(defn set-new-custom-drug [db [_ type value]]
  (assoc-in db [:ui :custom-drug type] value))

(defn fetch-new-custom-drug [db]
  (merge
   {:id (str (random-uuid))}
   (dissoc (get-in db [:ui :custom-drug]) :visible?)
   (timestamps)))

(defn save-custom-drug [{:keys [db]} _]
  (let [new-drug (fetch-new-custom-drug db)]
    (if (not (string/blank? (:drug-name new-drug)))
      {:db (assoc-in
            db
            [:store :patients (u/active-patient-id db)
             :prescription-drugs :custom-drugs (:id new-drug)]
            new-drug)
       :dispatch-n [[:hide-custom-drug-sheet]
                    [:persist-store]]}
      {:dispatch [:hide-custom-drug-sheet]})))

(defn register-events []
  (reg-event-db :initialize-db (fn [_ _] app-db))
  (reg-event-db :set-active-tab set-active-tab)
  (reg-event-fx :add-patient add-patient)
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
  (reg-event-fx :remove-custom-drug remove-custom-drug)
  (reg-event-fx :save-custom-drug save-custom-drug)
  (reg-event-db :ui-text-input-layout (assoc-into-db [:ui :text-input-layout]))
  (nav/register-events)
  (scan/register-events)
  (register/register-events)
  (search/register-events)
  (overdue/register-events))

(register-events)
