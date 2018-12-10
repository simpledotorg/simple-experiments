(ns simple-experiments.events.simple-card
  (:require [clojure.string :as string]
            [re-frame.core :refer [reg-event-fx]]
            [simple-experiments.events.navigation :as nav]))

(defn ->six-digit-id [card-uuid]
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
    :associated})

(defn has-six-digit-id? [patient six-digit-id]
  (let [six-digit-ids (set (concat (:six-digit-ids patient)
                                   (map ->six-digit-id
                                        (:card-uuids patient))))]
    (contains? six-digit-ids six-digit-id)))

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
        {:dispatch-n [[:goto :new-patient]
                      [:set-active-card nil six-digit-id :pending-registration]]}
        {:db (assoc-in db [:ui :patient-search :results] existing-patients)
         :dispatch-n [[:goto :patient-list]
                      [:goto-select-mode]
                      [:set-active-card nil six-digit-id :pending]]})

      :new-patient
      {:dispatch-n [[:set-active-card nil six-digit-id :pending-registration]
                    [:go-back]]}

      {})))

(defn set-active-card [{:keys [db]} [_ card-uuid six-digit-id status]]
  (let [sdid (or six-digit-id
                 (->six-digit-id card-uuid))]
    {:db (assoc-in db [:ui :active-card]
                   {:uuid card-uuid
                    :six-digit-id sdid
                    :six-digit-display (six-digit-display sdid)
                    :status status})}))

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
       (#{:pending :pending-registration :pending-association}
        (:status active-card))))

(defn pending-association? [active-card]
  (and (some? active-card)
       (= :pending-association (:status active-card))))

(defn pending-registration? [active-card]
  (and (some? active-card)
       (= :pending-registration (:status active-card))))

(defn register-events []
  (reg-event-fx :set-active-card set-active-card)
  (reg-event-fx :clear-active-card clear-active-card)
  (reg-event-fx :associate-simple-card-with-patient associate-simple-card-with-patient)
  (reg-event-fx :handle-six-digit-keyboard handle-six-digit-keyboard)
  (reg-event-fx :handle-six-digit-input handle-six-digit-input))
