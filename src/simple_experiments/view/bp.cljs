(ns simple-experiments.view.bp
  (:require
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [cljs-time.coerce :as timec]
   [clojure.string :as string]
   [simple-experiments.db.blood-pressure :as bp]
   [simple-experiments.view.components :as c]
   [simple-experiments.view.styles :as s]))

(defn row [{:keys [systolic diastolic] :as blood-pressure} today?]
  (let [risk-level (bp/risk-level blood-pressure)
        bp-color (if (>= (:numeric risk-level) 4)
                   (s/colors :high-bp)
                   (s/colors :normal-bp))]
    [c/view {:style {:flex-direction "row"
                     :align-items "center"}}
     [c/miconx {:name "heart"
                :color bp-color
                :size 22
                :style {:margin-right 10}}]
     [c/text
      {:style {:font-size 18
               :font-weight (if today? "bold" "normal")
               :margin-right 6
               :color bp-color
               :width 80}}
      (str systolic "/" diastolic)]
     [c/text
      {:style {:font-size 14
               :font-weight (if today? "bold" "normal")
               :text-align "left"
               :color bp-color}}
      (string/capitalize (:display risk-level))]]))

(defn empty-row []
  [c/view
   {:style {:flex-direction "row"
            :margin-bottom 16
            :height 30
            :border-bottom-width 1
            :border-bottom-color (s/colors :border)}}])

(defn bp-list [blood-pressures]
  [c/view
   {:style {:flex-direction "column"
            :margin-top 20}}
   (for [blood-pressure blood-pressures
         :let [days-ago (c/number-of-days-since
                         (timec/from-long (:created-at blood-pressure)))
               today? (= 0 days-ago)]]
     ^{:key (str (random-uuid))}
     [c/view
      {:style {:flex-direction "row"
               :margin-bottom 16
               :padding-bottom 8
               :justify-content "space-between"
               :align-items "center"
               :border-bottom-width 1
               :border-bottom-color (s/colors :border)}
       :ref       #(when today? (dispatch [:set-ref :first-bp %]))
       :on-layout #(when today? (dispatch [:measure :first-bp]))}
      [row blood-pressure today?]
      [c/text
       {:style {:font-size 16
                :font-weight (if today? "bold" "normal")}}
       (if today?
         "Today"
         (str days-ago " days ago"))]])
   (for [_ (range (max 0 (- 2 (count blood-pressures))))]
     ^{:key (str (random-uuid))}
     [empty-row])])

(defn history [blood-pressures]
  [c/view {:style {:padding-horizontal 32
                   :padding-vertical 16
                   :elevation 2}}
   [c/action-button-outline
    "heart-pulse"
    :community
    "New BP"
    #(dispatch [:show-bp-sheet])
    32]
   [bp-list blood-pressures]])

(defn bp-input [kind props]
  [c/view {:style {:align-items "center"}}
   [c/text-input
    (merge {:style {:font-size 40
                    :width 100
                    :text-align "center"}
            :underline-color-android (s/colors :border)
            :max-length 3
            :ref (fn [com]
                   (dispatch [:set-bp-ref kind com]))
            :keyboard-type "numeric"
            :on-change-text #(dispatch [:handle-bp-keyboard kind %])}
           props)]
   [c/text
    {:style {:font-size 16}}
    (string/capitalize (name kind))]])

(defn bp-sheet []
  (let [ui-bp (subscribe [:ui-bp])]
    (fn []
      [c/bottom-sheet
       {:height 180
        :close-action #(dispatch [:hide-bp-sheet])
        :visible? (:visible? @ui-bp)}

       [c/view {:style {:flex-direction "column"
                        :align-items "center"}}
        [c/text
         {:style {:font-size 16
                  :font-weight "bold"
                  :margin-vertical 20
                  :color (s/colors :primary-text)}}
         (string/upper-case "Enter blood pressure")]
        [c/view {:style {:flex-direction "row"}}
         [bp-input :systolic
          {:auto-focus (:visible? @ui-bp)
           :default-value (get-in @ui-bp [:value :systolic])}]
         [c/view {:style {:width 2
                          :height 64
                          :margin-horizontal 20
                          :transform [{:rotate "13deg"}]
                          :background-color (s/colors :border)}}]
         [bp-input :diastolic
          {:on-submit-editing #(dispatch [:save-bp])
           :default-value (get-in @ui-bp [:value :diastolic])}]]]])))
