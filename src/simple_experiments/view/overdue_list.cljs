(ns simple-experiments.view.overdue-list
  (:require [re-frame.core :refer [subscribe dispatch dispatch-sync]]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]
            [goog.string :as gstring]
            [goog.string.format]
            [simple-experiments.view.coach :as coach]
            [simple-experiments.view.components :as c]
            [simple-experiments.view.styles :as s]
            [simple-experiments.events.utils :as u]))

(defn call-in-text [{:keys [call-result call-in-days] :as patient}]
  (if (= :rescheduled call-result)
    (if (= "Tomorrow" call-in-days)
      "Call tomorrow"
      (gstring/format "Call in %s" call-in-days))
    "Call later..."))

(defn called-text [patient]
  (let [called-days-ago (-> (:called-at patient)
                            timec/from-long
                            (time/interval (time/now))
                            time/in-days)]
    [c/view {:style {:flex-direction "row"
                     :align-items "center"
                     :margin-top 5}}
     [c/micon {:name "call-made"
               :size     14
               :color    (s/colors :called)}]
     [c/text
      {:style {:font-size 14
               :color     (s/colors :called)
               :margin-left 2}}
      (str "Called " (string/lower-case (u/days-ago-text called-days-ago)))]]))

(defn call-result-label [{:keys [call-result] :as patient}]
  (let [expand?       (subscribe [:ui-overdue-list :expand (:id patient)])
        common-styles {:font-size          14
                       :color              (s/colors :primary-text)
                       :border-width       1
                       :border-radius      4
                       :margin-top         8
                       :padding-vertical   2
                       :padding-horizontal 8
                       :align-self         "flex-start"
                       :align-items        "center"
                       :justify-content    "center"}]
    (fn []
      (when (not @expand?)
        (case call-result
          :rescheduled
          [c/text
           {:style
            (merge common-styles {:border-color (s/colors :yellow)})}
           (call-in-text patient)]

          :agreed-to-return
          [c/text
           {:style
            (merge common-styles {:background-color (s/colors :called)
                                  :border-width 0
                                  :color (s/colors :white)})}
           "Agreed to return"]

          nil)))))

(defn patient-details [{:keys [full-name age gender phone-number] :as patient}]
  (let [latest-bp (u/latest-bp patient)]
    [c/view
     {:style {:flex 1}}
     [c/view {:flex-direction "row"
              :align-items    "center"}
      [c/text
       {:style {:font-size 16
                :color     (s/colors :primary-text)}}
       full-name]
      [c/text
       {:style {:margin-left 4
                :font-size   14
                :color       (s/colors :light-text)}}
       (str "(" (string/capitalize (str gender)) ", " age ")")]]
     [c/text
      {:style {:margin-top 4
               :font-size  14
               :color      (s/colors :light-text)}}
      (str (u/days-ago-text (u/last-visit patient)) ": "
           (:systolic latest-bp) "/" (:diastolic latest-bp))]
     [c/view
      {:style {:flex-direction "row"}}
      [c/text
       {:style {:margin-top 4
                :margin-right 8
                :font-size  14
                :color      (s/colors :overdue)}}
       (str (:overdue-days patient) " days overdue")]
      (when (:called-at patient)
        [called-text patient])]
     [call-result-label patient]]))

(defn action-item [icon-name text & [icon-color]]
  [c/view {:flex-direction "row"
           :align-items "center"
           :margin-vertical 10}
   [c/micon {:name  icon-name
             :size  24
             :color (or icon-color (s/colors :light-text))}]
   [c/text
    {:style {:font-size 16
             :margin-left 12
             :color (s/colors :primary-text)}}
    text]])

(defn call-result-actions [{:keys [call-in-days call-result] :as patient}]
  (let [rescheduled?      (= :rescheduled call-result)
        agreed-to-return? (= :agreed-to-return call-result)
        common-style      {:flex             1
                           :border-radius    4
                           :padding-vertical 6
                           :align-items      "center"
                           :justify-content  "center"}
        inactive-style    {:border-color (s/colors :dark-border)
                           :border-width 1}
        active-style      {:border-width 0}]
    [c/view
     {:style {:flex-direction  "row"
              :align-items     "center"
              :justify-content "center"
              :margin-bottom   16}}
     [c/touchable-opacity
      {:on-press #(dispatch [:agreed-to-return patient])
       :style    (merge common-style
                        {:margin-right     16
                         :background-color (if agreed-to-return?
                                             (s/colors :called)
                                             "transparent")}
                        (if agreed-to-return?
                          active-style
                          inactive-style))}
      [c/text {:style {:color     (if agreed-to-return?
                                    (s/colors :white)
                                    (s/colors :light-text))
                       :font-size 14}}
       "Agreed to return"]]
     [c/touchable-opacity
      {:on-press #(dispatch [:call-later patient])
       :style    (merge common-style
                        {:background-color (if rescheduled?
                                             (s/colors :yellow)
                                             "transparent")}
                        (if rescheduled?
                          active-style
                          inactive-style))}
      [c/text {:style {:color     (s/colors :primary-text)
                       :font-size 14}}
       (call-in-text patient)]]]))

(defn expanded-view [patient]
  (let [see-phone-number? (subscribe [:ui-overdue-list :see-phone-number? (:id patient)])]
    [c/view
     {:style {:flex 1
              :margin-top 20}}
     [call-result-actions patient]
     [c/touchable-opacity
      {:on-press #(dispatch [:see-phone-number patient])}
      [action-item "contact-phone" (if @see-phone-number?
                                     (str "+91 " (:phone-number patient))
                                     "See phone number")]]
     [c/touchable-opacity
      {:on-press #(dispatch [:set-active-patient-id (:id patient)])}
      [action-item "assignment" "See patient record"]]
     [c/touchable-opacity
      {:on-press #(dispatch [:show-skip-reason-sheet patient])}
      [action-item "cancel" "Remove patient from list..." (s/colors :error)]]]))

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
       {:on-press #(dispatch [:expand-overdue-card patient])
        :style {:flex 1}}
       [patient-details patient]]
      [c/view
       {:style {:border-left-width (if @expand? 0 1)
                :border-left-color (s/colors :border)
                :justify-content "center"}}
       [c/touchable-opacity
        {:on-press #(dispatch [:make-call patient])
         :style {:padding-horizontal 12}}
        [c/micon {:name  "call"
                  :size  24
                  :color (s/colors :primary-text)}]]]]
     (if @expand?
       [expanded-view patient])]))

(defn chip [text active? on-press]
  [c/touchable-opacity
   {:on-press on-press
    :style    {:margin-horizontal  5
               :border-width       1
               :border-color       (if active?
                                     (s/colors :accent)
                                     (s/colors :dark-border))
               :border-radius      15
               :background-color   (if active?
                                     (s/colors :accent)
                                     "transparent")
               :padding-horizontal 12
               :padding-vertical   6}}
   [c/text
    {:style {:font-size  14
             :color      (if active?
                           (s/colors :white)
                           (s/colors :light-text))
             :min-width  30
             :text-align "center"}}
    text]])

(defn filters []
  (let [filter-by (subscribe [:ui-overdue-list :filter-by])]
    [c/view
     {:style {:flex-direction "row"
              :margin-bottom  20
              :align-items    "center"}}
     [c/text
      {:style {:font-size 14}}
      (string/upper-case "Overdue by: ")]
     [chip "All" (or (= @filter-by :all) (nil? @filter-by))
      #(dispatch [:set-overdue-filter :all])]
     [chip "1 to 10 days" (= @filter-by :one-to-ten)
      #(dispatch [:set-overdue-filter :one-to-ten])]]))

(defn reason-row [patient reason title active? & {:keys [style]}]
  [c/touchable-opacity
   {:on-press #(dispatch [:select-skip-reason patient reason])
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

(defn stepper []
  (let [current-step (subscribe [:ui-overdue-list :reschedule-stepper :current-step])]
    [c/view
     {:style {:flex-direction "row"
              :margin-vertical 8
              :align-items "center"
              :justify-content "center"}}
     [c/touchable-opacity
      {:on-press #(dispatch [:reschedule-stepper :previous])}
      [c/micon {:name "remove-circle-outline"
                :size 24
                :color (s/colors :light-text)
                :style {:padding 40}}]]
     [c/text
      {:style {:font-size 34
               :width "50%"
               :color (s/colors :primary-text)
               :text-align "center"}}
      (or @current-step "2 days")]
     [c/touchable-opacity
      {:on-press #(dispatch [:reschedule-stepper :next])}
      [c/micon {:name "add-circle-outline"
                :size 24
                :color (s/colors :light-text)
                :style {:padding 40}}]]]))

(defn reschedule-sheet [active-patient]
  (let [show?      (subscribe [:ui-overdue-list :show-reschedule-sheet?])
        next-visit (subscribe [:ui-overdue-list :next-visit])
        patient    (subscribe [:ui-overdue-list :reschedule-patient])]
    (fn []
      [c/modal {:animation-type   "slide"
                :transparent      true
                :visible          (true? @show?)
                :on-request-close #(dispatch [:hide-reschedule-sheet])}
       [c/view
        {:style {:flex             1
                 :background-color "#000000AA"}}
        [c/touchable-opacity
         {:on-press #(dispatch [:hide-reschedule-sheet])
          :style    {:height          "60%"
                     :justify-content "flex-end"
                     :align-items     "center"
                     :padding-bottom  20}}]
        [c/view
         {:style {:background-color (s/colors :white)
                  :justify-content  "center"
                  :align-items      "center"
                  :flex             1
                  :padding          20
                  :border-radius    5}}
         [c/text
          {:style {:width               "100%"
                   :text-align          "center"
                   :font-size           16
                   :font-weight         "bold"
                   :color               (s/colors :primary-text)
                   :padding-bottom      10
                   :border-bottom-color (s/colors :border)
                   :border-bottom-width 1}}
          "Call later in"]

         [stepper]

         [c/view
          {:style {:flex-direction "row"}}
          [c/floating-button
           {:title    "Do not schedule"
            :on-press #(do
                         (dispatch [:clear-reschedule @patient])
                         (dispatch [:hide-reschedule-sheet]))
            :style    {:flex             1
                       :background-color (s/colors :white)
                       :color            (s/colors :accent)
                       :border-color     (s/colors :accent)
                       :border-width     1
                       :height           48
                       :border-radius    3
                       :elevation        1
                       :font-size        14
                       :margin-right     16}}]
          [c/floating-button
           {:title    "Done"
            :on-press #(do (dispatch [:reschedule @patient])
                           (dispatch [:hide-reschedule-sheet]))
            :style    {:flex          1
                       :height        48
                       :border-radius 3
                       :elevation     1
                       :font-size     14}}]]]]])))

(defn skip-reason-sheet []
  (let [show?       (subscribe [:ui-overdue-list :show-skip-reason-sheet?])
        skip-reason (subscribe [:ui-overdue-list :skip-reason])
        patient     (subscribe [:ui-overdue-list :skip-patient])]
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
         [reason-row @patient :out-of-area "Patient has moved out of the area"
          (= @skip-reason :out-of-area)]
         [reason-row @patient :no-response "Patient is not responding"
          (= @skip-reason :no-response)]
         [reason-row @patient :died "Patient died"
          (= @skip-reason :died)]
         [reason-row @patient :other "Other reason"
          (= @skip-reason :other)
          :style {:border-bottom-width 0}]
         [c/floating-button
          {:title    "Skip Calling Patient"
           :on-press #(dispatch [:set-skip-reason @patient])
           :style    {:height        48
                      :border-radius 3
                      :elevation     1
                      :font-weight   "500"
                      :font-size     18}}]]]])))

(defn content []
  (let [patients (subscribe [:overdue-patients])
        coach?   (subscribe [:ui-coach :overdue])]
    (fn []
      [c/view
       {:style {:background-color (s/colors :window-backround)}}
       [c/scroll-view
        {:content-container-style {:margin 16}}
        (when-not (empty? @patients)
          [filters])
        [c/view {:style {:flex          1
                         :margin-bottom 50}}
         (for [patient @patients]
           ^{:key (str (random-uuid))}
           [overdue-patient-card patient])
         (when (empty? @patients)
           [c/image {:source    c/overdue-empty
                     :resize-mode "contain"
                     :style       {:width "100%"
                                   :height (:width c/dimensions)}}])]
        [skip-reason-sheet]
        [reschedule-sheet]]])))
