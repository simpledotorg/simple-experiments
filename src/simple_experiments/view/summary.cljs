(ns simple-experiments.view.summary
  (:require
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [cljs-time.core :as time]
   [cljs-time.coerce :as timec]
   [clojure.string :as string]
   [simple-experiments.db.patient :as db]
   [simple-experiments.view.bp :as bp]
   [simple-experiments.view.components :as c]
   [simple-experiments.view.styles :as s]
   [simple-experiments.events.utils :as u]))

(defn summary-header [{:keys [full-name birth-year gender village-or-colony phone-number]}]
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
     (str (string/capitalize gender) ", " (u/age birth-year) " • " phone-number)]
    [c/text
     {:style {:color "white" :font-size 16}}
     village-or-colony]]
   [c/touchable-opacity
    {:on-press #()
     :style {:background-color "rgba(0, 0, 0, 0.16)"
             :border-radius 2
             :position "absolute"
             :right 20
             :top 24
             :padding-vertical 3
             :padding-horizontal 8}}
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
   [c/action-button-outline
    "local-pharmacy"
    :regular
    (if (not-empty drugs) "Update Medicines" "Add Medicines")
    #(dispatch [:goto :prescription-drugs])
    42]])

(defn radio [active?]
  [c/micon {:name  (if active?
                     "radio-button-checked"
                     "radio-button-unchecked")
            :size  22
            :color (if active?
                     (s/colors :accent)
                     (s/colors :placeholder))}])

(defn schedule-row [days active? & {:keys [style]}]
  [c/touchable-opacity
   {:on-press #(dispatch [:schedule-next-visit days])
    :style (merge {:flex-direction      "row"
                   :justify-content     "space-between"
                   :margin-vertical     10
                   :border-bottom-color (s/colors :border)
                   :border-bottom-width 1
                   :padding-bottom      15}
                  style)}
   [c/text
    {:style {:font-size 18}}
    (if (some? days) (str days " Days") "Do not schedule")]
   [radio active? #()]])

(defn schedule-sheet [active-patient]
  (let [show? (subscribe [:ui-new-patient :show-schedule-sheet?])
        next-visit (subscribe [:ui-new-patient :nex-visit])]
    (fn []
      [c/modal {:animation-type   "fade"
                :transparent      true
                :visible          (true? @show?)
                :on-request-close #()}
       [c/view
        {:style {:flex             1
                 :background-color "#000000AA"}}
        [c/view
         { :style {:height          "50%"
                   :justify-content "flex-end"
                   :align-items     "center"
                   :padding-bottom   20}}
         [c/micon {:name  "check"
                   :size  128
                   :color (s/colors :green)}]
         [c/text
          {:style {:font-size 32
                   :color     (s/colors :green)}}
          "Saved"]]
        [c/view
         {:style {:background-color (s/colors :white)
                  :justify-content  "center"
                  :flex             1
                  :padding          20
                  :border-radius    5}}
         [c/text
          {:style {:font-size           20
                   :font-weight         "bold"
                   :color               (s/colors :primary-text)
                   :padding-bottom      10
                   :border-bottom-color (s/colors :border)
                   :border-bottom-width 1}}
          "Schedule Next Visit In"]
         [schedule-row 5 (= @next-visit 5)]
         [schedule-row 30 (= @next-visit 30)]
         [schedule-row :none (= @next-visit :none)
          :style {:border-bottom-width 0}]
         [c/floating-button
          {:title    "Done"
           :on-press #(dispatch [:go-back])
           :style    {:height        48
                      :border-radius 3
                      :elevation     1
                      :font-weight   "500"
                      :font-size     18}}]]]])))

(defn save-button []
  [c/view {:style {:height 90
                   :elevation 20
                   :background-color (s/colors :sheet-background)
                   :justify-content "center"}}
   [c/floating-button
    {:title "Save"
     :on-press #(dispatch [:show-schedule-sheet])
     :style {:height 48
             :margin-horizontal 48
             :border-radius 3
             :elevation 1
             :font-weight "500"
             :font-size 18}}]])

(defn page []
  (let [active-patient-id (subscribe [:active-patient-id])
        active-patient    (subscribe [:patients @active-patient-id])]
    (fn []
      [c/view {:style {:flex 1}}
       [c/scroll-view
        {:sticky-header-indices [0]}
        [summary-header @active-patient]
        [c/view
         {:style {:flex            1
                  :flex-direction  "column"
                  :justify-content "flex-start"
                  :margin-bottom   20}}
         [prescription (:prescription-drugs @active-patient)]
         [c/shadow-line]
         [bp/history (:blood-pressures @active-patient)]
         [bp/bp-sheet]
         [schedule-sheet @active-patient]]]
       [save-button]])))
