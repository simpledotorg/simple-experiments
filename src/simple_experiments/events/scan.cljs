(ns simple-experiments.events.scan
  (:require [re-frame.core :refer [reg-event-db reg-event-fx dispatch]]
            [re-frame-fx.dispatch]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [cljs-time.format :as timef]
            [clojure.string :as string]
            [simple-experiments.db :as db :refer [app-db]]
            [simple-experiments.db.patient :as db-p]
            [simple-experiments.events.utils :refer [assoc-into-db]]))

(defn show-camera [{:keys [db]} _]
  {:db (assoc-in db [:home :show-camera?] true)})

(defn hide-camera [{:keys [db]} _]
  {:db (assoc-in db [:home :show-camera?] false)})

(defonce parse-string
  (.-parseString (js/require "react-native-xml2js")))

(defn patient-from-qr [{:keys [yob state street loc dist gender name dob] :as qr-data}]
  (let [dob-date (when dob (timef/parse (timef/formatter "dd/MM/yyyy") dob))
        yob-date (when yob (timef/parse (timef/formatter "yyyy") yob))]
    {:gender            (case gender "M" "male" "F" "female")
     :full-name         name
     :age               (time/in-years (time/interval (or dob-date yob-date) (time/now)))
     :phone-number      (or (:phone-number qr-data) "9990000000")
     :street-name       street
     :village-or-colony loc
     :district          dist
     :state             state}))

(def id-fields
  #{:full-name :age :gender :village-or-colony})

(defn find-patient [db patient]
  (first
   (filter
    (fn [p]
      (= (select-keys patient id-fields)
         (select-keys p id-fields)))
    (vals (get-in db [:store :patients])))))

(defn handle-scan [{:keys [db]} [_ qr-data]]
  (let [patient (patient-from-qr qr-data)
        _ (def *p patient)
        existing-patient (find-patient db patient)]
    (if (nil? existing-patient)
      {:db             (-> db
                           (assoc-in [:ui :new-patient :values] patient)
                           (assoc-in [:ui :new-patient :valid?] true))
       :dispatch-later [{:ms 200 :dispatch [:register-new-patient]}]}
      {:dispatch-n [[:set-active-patient-id (:id existing-patient)]
                    [:show-bp-sheet]]})))

(defn parse-qr [{:keys [db]} [_ event]]
  (let [data-str (:data (js->clj event :keywordize-keys true))]
    (def *ds data-str)
    (parse-string data-str
                  (fn [err result]
                    (let [qr-data (get-in (js->clj result :keywordize-keys true)
                                          [:PrintLetterBarcodeData :$])]
                      (dispatch [:handle-scan qr-data]))))
    {}))

(defn register-events []
  (reg-event-fx :show-camera show-camera)
  (reg-event-fx :hide-camera hide-camera)
  (reg-event-fx :parse-qr parse-qr)
  (reg-event-fx :handle-scan handle-scan))