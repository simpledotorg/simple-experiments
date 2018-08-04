(ns simple-experiments.view.overdue-list
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.events.utils :as u]))

(defn called-text [patient]
  (let [called-days-ago (-> (:called-at patient)
                            timec/from-long
                            (time/interval (time/now))
                            time/in-days)]
    [c/view {:style {:flex-direction "row"
                     :align-items "center"
                     :margin-top 5}}
     [c/micon {:name "call-made"
               :size     16
               :color    (s/colors :called)}]
     [c/text
      {:style {:font-size 16
               :color     (s/colors :called)
               :margin-left 2}}
      (str "Called " (string/lower-case (u/days-ago-text called-days-ago)))]]))

(defn patient-details [{:keys [full-name birth-year gender phone-number] :as patient}]
  (let [latest-bp (u/latest-bp patient)]
    [c/view
     {:style {:flex 1}}
     [c/view {:flex-direction "row"
              :align-items    "center"}
      [c/text
       {:style {:font-size 18
                :color     (s/colors :primary-text)}}
       full-name]
      [c/text
       {:style {:margin-left 10
                :font-size   16
                :color       (s/colors :light-text)}}
       (str "(" (string/capitalize gender) ", " (u/age birth-year) ")")]]
     [c/text
      {:style {:margin-top 4
               :font-size  16
               :color      (s/colors :light-text)}}
      (str (u/days-ago-text (u/last-visit patient)) ": "
           (:systolic latest-bp) "/" (:diastolic latest-bp))]
     [c/view
      {:style {}}
      [c/text
       {:style {:margin-top 4
                :font-size  16
                :color      (s/colors :overdue)}}
       (str (:overdue-days patient) " days overdue")]
      (when (:called-at patient)
        [called-text patient])]]))

(defn action-item [icon-name text & [icon-color]]
  [c/view {:flex-direction "row"
           :align-items "center"
           :margin-vertical 10}
   [c/micon {:name  icon-name
             :size  30
             :color (or icon-color (s/colors :primary-text))}]
   [c/text
    {:style {:font-size 18
             :margin-left 10
             :color (s/colors :primary-text)}}
    text]])

(defn expanded-view [patient]
  (let [see-phone-number? (subscribe [:ui-overdue-list :see-phone-number? (:id patient)])]
    [c/view
     {:style {:flex 1
              :margin-top 20
              :border-top-width 1
              :border-top-color (s/colors :border)}}
     [c/touchable-opacity
      {:on-press #(dispatch [:see-phone-number patient])}
      [action-item "contact-phone" (if @see-phone-number?
                                     (str "+91 " (:phone-number patient))
                                     "See phone number")]]
     [c/touchable-opacity
      {:on-press #(dispatch [:set-active-patient-id (:id patient)])}
      [action-item "assignment" "See patient record"]]
     [c/touchable-opacity
      {:on-press #(c/toast "Reminder set for 5 days from now.")}
      [action-item "add-alarm" "Remind to call in 5 days"]]
     [c/touchable-opacity
      {:on-press #(dispatch [:show-skip-reason-sheet patient])}
      [action-item "close" "Skip calling" (s/colors :error)]]]))

(defn overdue-patient-card [patient]
  (let [expand? (subscribe [:ui-overdue-list :expand (:id patient)])]
    [c/view
     {:style {:flex 1
              :elevation        2
              :margin-vertical  5
              :padding          10
              :border-radius    4
              :border-width     1
              :border-color     (s/colors :light-border)
              :background-color (s/colors :white)}}
     [c/view
      {:style {:flex             1
               :flex-direction   "row"
               :justify-content  "space-between"}}
      [c/touchable-opacity
       {:on-press #(dispatch [:expand-overdue-card patient])}
       [patient-details patient]]
      [c/view
       {:style {:border-left-width (if @expand? 0 1)
                :border-left-color (s/colors :border)
                :justify-content "center"}}
       [c/touchable-opacity
        {:on-press #(dispatch [:make-call patient])
         :style {:padding-horizontal 12}}
        [c/micon {:name  "call"
                  :size  28
                  :color (s/colors :primary-text)}]]]]
     (if @expand?
       [expanded-view patient])]))

(defn chip [text active? on-press]
  [c/touchable-opacity
   {:on-press on-press
    :style    {:margin-horizontal  5
               :border-radius      15
               :background-color   (if active?
                                     (s/colors :accent)
                                     (s/colors :pale-gray))
               :padding-horizontal 10
               :padding-vertical 4}}
   [c/text
    {:style {:font-size 16
             :color     (if active?
                          (s/colors :white)
                          (s/colors :accent))
             :min-width 30
             :text-align "center"}}
    text]])

(defn filters []
  (let [filter-by (subscribe [:ui-overdue-list :filter-by])]
    [c/view
     {:style {:flex-direction "row"
              :margin-bottom  20
              :align-items    "center"}}
     [c/text
      {:style {:font-size 16}}
      (string/upper-case "Overdue by: ")]
     [chip "All" (or (= @filter-by :all) (nil? @filter-by))
      #(dispatch [:set-overdue-filter :all])]
     [chip "1 to 10 days" (= @filter-by :one-to-ten)
      #(dispatch [:set-overdue-filter :one-to-ten])]]))

(defn reason-row [patient reason title active? & {:keys [style]}]
  [c/touchable-opacity
   {:on-press #(dispatch [:set-skip-reason patient reason])
    :style (merge {:flex-direction      "row"
                   :justify-content     "space-between"
                   :padding-vertical     15
                   :border-bottom-color (s/colors :border)
                   :border-bottom-width 1}
                  style)}
   [c/text
    {:style {:font-size 18
             :color (if active?
                      (s/colors :primary-text)
                      (s/colors :light-text))}}
    title]
   [c/radio active?]])

(defn skip-reason-sheet []
  (let [show?       (subscribe [:ui-overdue-list :show-skip-reason-sheet?])
        skip-reason (subscribe [:ui-overdue-list :skip-reason])
        patient (subscribe [:ui-overdue-list :skip-patient])]
    (fn []
      [c/modal {:animation-type   "slide"
                :transparent      true
                :visible          (true? @show?)
                :on-request-close #(dispatch [:hide-skip-reason-sheet])}
       [c/view
        {:style {:flex             1
                 :justify-content  "flex-end"
                 :background-color "#00000066"}}
        [c/touchable-opacity
         {:style    {:flex 1}
          :on-press #(dispatch [:hide-skip-reason-sheet])}]
        [c/view
         {:style {:background-color (s/colors :white)
                  :justify-content  "center"
                  :padding          20
                  :border-radius    5}}
         [c/text
          {:style {:font-size           20
                   :font-weight         "bold"
                   :color               (s/colors :primary-text)
                   :padding-bottom      10
                   :border-bottom-color (s/colors :border)
                   :border-bottom-width 1}}
          "Select a Reason"]
         [reason-row @patient :visited "Patient has already visited clinic"
          (= @skip-reason :visited)]
         [reason-row @patient :no-response "Patient has moved out of the area"
          (= @skip-reason :no-response)]
         [reason-row @patient :out-of-area "Patient is not responding"
          (= @skip-reason :out-of-area)]
         [reason-row @patient :died "Patient died"
          (= @skip-reason :died)]
         [reason-row @patient :other "Other reason"
          (= @skip-reason :other)
          :style {:border-bottom-width 0}]
         [c/floating-button
          {:title    "Skip Calling Patient"
           :on-press #(dispatch [:hide-skip-reason-sheet])
           :style    {:height        48
                      :border-radius 3
                      :elevation     1
                      :font-weight   "500"
                      :font-size     18}}]]]])))

(defn content []
  (let [patients (subscribe [:overdue-patients])]
    (fn []
      [c/scroll-view
       {:content-container-style {:margin 16}}
       [filters]
       [c/view {:style {:flex 1
                        :margin-bottom 50}}
        (for [patient @patients]
          ^{:key (str (random-uuid))}
          [overdue-patient-card patient])
        (when (empty? @patients)
          [c/text
           {:style {:font-size 24
                    :color (s/colors :disabled)
                    :align-self "center"
                    :margin-top 200}}
           "No patients overdue"])]
       [skip-reason-sheet]])))
