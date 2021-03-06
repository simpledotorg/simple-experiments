(ns simple-experiments.events.simple-card
  (:require [clojure.string :as string]
            [re-frame.core :refer [reg-event-fx]]
            [simple-experiments.events.navigation :as nav]))

(defn uuid->six-digit-id [card-uuid]
  (->> (str card-uuid)
       (re-seq #"\d")
       (take 6)
       (apply str)))

(defn six-digit-display [six-digit-str]
  (let [[head tail] (split-at 3 six-digit-str)]
    (string/join (concat head [" "] tail))))

(def active-card-statuses
  #{:pending
    :pending-association
    :pending-registration
    :pending-selection
    :associated})

(defn six-digit-ids [patient]
  (set (concat (:six-digit-ids patient)
               (map uuid->six-digit-id
                    (:card-uuids patient)))))

(defn has-six-digit-id? [patient six-digit-id]
  (contains? (six-digit-ids patient)
             six-digit-id))

(defn find-patients [db six-digit-id]
  (->> db
       :store
       :patients
       vals
       (filter #(has-six-digit-id? % six-digit-id))))

(defn handle-six-digit-keyboard [{:keys [db]} [_ six-digit-id]]
  {:db (assoc-in db [:ui :simple-card :six-digit-id] six-digit-id)})

(defn handle-six-digit-input [{:keys [db]} [_]]
  (let [six-digit-id (get-in db [:ui :simple-card :six-digit-id])
        existing-patients (find-patients db six-digit-id)]
    (case (nav/previous-screen)
      :home
      (if (empty? existing-patients)
        {:dispatch-n [[:goto :patient-search]
                      [:patient-search-clear]
                      [:set-active-card nil six-digit-id :pending-association]]}
        {:db (assoc-in db [:ui :patient-search :results] existing-patients)
         :dispatch-n [[:goto :patient-list]
                      [:set-active-card nil six-digit-id :pending-selection]]})

      :new-patient
      {:dispatch-n [[:set-active-card nil six-digit-id :pending-registration]
                    [:go-back]]}

      :edit-patient
      {:dispatch-n [[:set-active-card nil six-digit-id :pending-update]
                    [:go-back]]}

      {})))

(defn card
  ([card-uuid]
   (card card-uuid nil))
  ([card-uuid six-digit-id]
   (card card-uuid nil :associated))
  ([card-uuid six-digit-id status]
   (let [sdid (or six-digit-id
                  (uuid->six-digit-id card-uuid))]
     {:uuid card-uuid
      :six-digit-id sdid
      :six-digit-display (six-digit-display sdid)
      :status status})))

(defn set-active-card [{:keys [db]} [_ card-uuid six-digit-id status]]
  {:db (assoc-in db [:ui :active-card] (card card-uuid six-digit-id status))})

(defn update-active-card-status [{:keys [db]} [_ status]]
  (if (some? (get-in db [:ui :active-card]))
    {:db (assoc-in db [:ui :active-card :status] status)}
    {}))

(defn clear-active-card [{:keys [db]} _]
  {:db (assoc-in db [:ui :active-card] nil)})

(defn associate-simple-card-with-patient [{:keys [db]} [_ card patient-id]]
  (let [uuid (:uuid card)
        sdid (:six-digit-id card)
        field-to-update (if uuid :card-uuids :six-digit-ids)
        update-with-value (or uuid sdid)]
    {:db (-> db
             (update-in [:store :patients patient-id field-to-update] conj update-with-value)
             (assoc-in [:ui :active-card :status] :associated))}))

(defn pending? [active-card]
  (and (some? active-card)
       (#{:pending :pending-registration :pending-association :pending-selection :pending-update}
        (:status active-card))))

(defn pending-association? [active-card]
  (and (some? active-card)
       (= :pending-association (:status active-card))))

(defn pending-registration? [active-card]
  (and (some? active-card)
       (= :pending-registration (:status active-card))))

(defn pending-update? [active-card]
  (and (some? active-card)
       (= :pending-update (:status active-card))))

(defn register-events []
  (reg-event-fx :set-active-card set-active-card)
  (reg-event-fx :update-active-card-status update-active-card-status)
  (reg-event-fx :clear-active-card clear-active-card)
  (reg-event-fx :associate-simple-card-with-patient associate-simple-card-with-patient)
  (reg-event-fx :handle-six-digit-keyboard handle-six-digit-keyboard)
  (reg-event-fx :handle-six-digit-input handle-six-digit-input))
