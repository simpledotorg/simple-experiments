(ns simple-experiments.view.summary
  (:require
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [cljs-time.core :as time]
   [cljs-time.coerce :as timec]
   [clojure.string :as string]
   [simple-experiments.db.patient :as db]
   [simple-experiments.view.bp :as bp]
   [simple-experiments.view.components :as c]
   [simple-experiments.view.styles :as s]))

(defn summary-header [{:keys [full-name age gender street-name
                              village-or-colony phone-number]}]
  [c/view {:style {:flex-direction "row"
                   :background-color (s/colors :primary)
                   :padding-horizontal 16
                   :padding-vertical 20
                   :align-items "flex-start"
                   :justify-content "flex-start"
                   :elevation 10}}
   [c/touchable-opacity
    {:on-press #(dispatch [:go-back])}
    [c/micon {:name "arrow-back"
              :size 28
              :color (s/colors :white)
              :style {:margin-right 16
                      :margin-top 2}}]]
   [c/view
    {:style {:flex-direction "column"}}
    [c/text
     {:style {:color "white" :font-size 24 :font-weight "bold"}}
     (string/capitalize full-name)]
    [c/text
     {:style {:color "white" :font-size 16}}
     (str (string/capitalize gender) ", " age " • " phone-number)]
    [c/text
     {:style {:color "white" :font-size 16}}
     (if (string/blank? street-name)
       village-or-colony
       (str street-name ", " village-or-colony))]]
   [c/touchable-opacity
    {:on-press #(c/alert "Edit patient details.")
     :style {:background-color "rgba(0, 0, 0, 0.16)"
             :position "absolute"
             :right 20
             :top 24
             :padding-vertical 2
             :padding-horizontal 6}}
    [c/text
     {:style {:color "white"
              :font-size 16}}
     (string/upper-case "Edit")]]])

(defn drug-row [{:keys [drug-name drug-dosage]}]
  [c/view {:style {:flex-direction   "row"
                   :justify-content  "flex-start"
                   :padding-vertical 8}}
   [c/text
    {:style {:font-size    20
             :font-weight  "bold"
             :margin-right 10
             :width        80
             :color        (s/colors :primary-text)}}
    (string/capitalize (or drug-dosage ""))]
   [c/text
    {:style {:font-size 20
             :color     (s/colors :primary-text-2)}}
    (string/capitalize (name drug-name))]])

(defn latest-drug-time [{:keys [custom-drugs protocol-drugs] :as drugs}]
  (->> (map :updated-at custom-drugs)
       (cons (:updated-at protocol-drugs))
       sort
       last))

(defn drugs-updated-since [drugs]
  (let [updated-days-ago (c/number-of-days-since
                          (timec/from-long (latest-drug-time drugs)))]
    [c/view {:style {:flex-direction  "column"
                     :justify-content "center"
                     :align-items     "flex-end"}}
     [c/text {:style {:font-size 16}} "Updated"]
     [c/text {:style {:font-size 18}}
      (if (= 0 updated-days-ago)
        "Today"
        (str updated-days-ago " days ago"))]]))

(defn all-drug-details [{:keys [custom-drugs protocol-drugs] :as drugs}]
  (concat
   (map db/protocol-drugs-by-id (:drug-ids protocol-drugs))
   (vals custom-drugs)))

(defn drugs-list [drugs]
  [c/view {:style {:flex-direction  "row"
                   :justify-content "space-between"}}
   [c/view {:style {:flex-direction "column"}}
    (for [drug-details (all-drug-details drugs)]
      ^{:key (str (random-uuid))}
      [drug-row drug-details])]
   (when (seq drugs)
     [drugs-updated-since drugs])])

(defn prescription [drugs]
  [c/view {:style {:padding 32}}
   [drugs-list drugs]
   [c/action-button
    "local-pharmacy"
    :regular
    (if (not-empty drugs) "Update Medicines" "Add Medicines")
    #(dispatch [:goto :prescription-drugs])
    42]])

(defn page []
  (let [active-patient-id (subscribe [:active-patient-id])
        active-patient (subscribe [:patients @active-patient-id])]
    (fn []
      [c/scroll-view
       [summary-header @active-patient]
       [c/view
        [prescription (:prescription-drugs @active-patient)]
        [c/shadow-line]
        [bp/history (:blood-pressures @active-patient)]
        [bp/bp-sheet]]])))
