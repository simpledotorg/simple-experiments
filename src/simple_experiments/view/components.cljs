(ns simple-experiments.view.components
  (:require [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch]]
            [simple-experiments.view.styles :as s]
            [clojure.string :as string]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]))

(def ReactNative (js/require "react-native"))
(def micon (-> "react-native-vector-icons/MaterialIcons"
               js/require
               .-default
               r/adapt-react-class))
(def miconx (-> "react-native-vector-icons/MaterialCommunityIcons"
                js/require
                .-default
                r/adapt-react-class))

(def dimensions
  (-> (.-Dimensions ReactNative)
      (.get "window")
      (js->clj :keywordize-keys true)))

(def Animated (.-Animated ReactNative))
(def timing (.-timing Animated))
(def text (r/adapt-react-class (.-Text ReactNative)))
(def atext (r/adapt-react-class (.-Text Animated)))
(def modal (r/adapt-react-class (.-Modal ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def button (r/adapt-react-class (.-Button ReactNative)))
(def text-input (r/adapt-react-class (.-TextInput ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def touchable-opacity (r/adapt-react-class (.-TouchableOpacity ReactNative)))
(def status-bar (r/adapt-react-class (.-StatusBar ReactNative)))
(def back-handler (.-BackHandler ReactNative))
(def keyboard (.-Keyboard ReactNative))
(def scan-illustration (js/require "./images/scan_illustration.png"))

(defn alert
  ([title]
   (.alert (.-Alert ReactNative) title))
  ([title message actions options]
   (.alert (.-Alert ReactNative)
           title
           message
           (clj->js actions)
           (clj->js options))))

(defn number-of-days-since [in-time]
  (time/in-days (time/interval in-time (time/now))))

(defn screen [display-name component on-back]
  (let [on-back (fn [] (on-back back-handler)
                  true)]
    (r/create-class
     {:display-name display-name

      :component-did-mount
      (fn [] (.addEventListener
              back-handler
              "hardwareBackPress"
              on-back))

      :component-will-unmount
      (fn [] (.removeEventListener
              back-handler
              "hardwareBackPress"
              on-back))

      :reagent-render
      (fn []
        [view {:style {:flex 1}}
         [status-bar {:background-color (s/colors :primary-dark)}]
         [component]])})))

(defn shadow-line []
  [view {:elevation 2
         :height 1
         :border-bottom 1
         :border-bottom-color "transparent"}])

(defn action-button [icon-name icon-family title action height]
  (let [icon (case icon-family
               :regular micon
               :community miconx)]
    [touchable-opacity
     {:on-press action
      :style {:margin-top 20
              :background-color (s/colors :accent)
              :border-radius 3
              :elevation 1
              :height height
              :flex-direction "row"
              :align-items "center"
              :justify-content "center"}}
     [icon {:name icon-name
            :size 26
            :color (s/colors :white)
            :style {:margin-right 10}}]
     [text {:style {:color (s/colors :white)
                    :font-size 16
                    :font-weight "500"}}
      (string/upper-case title)]]))

(defn floating-button [{:keys [on-press title style icon] :as props}]
  [touchable-opacity
   {:on-press on-press
    :color    (s/colors :accent)
    :style    (merge {:background-color (s/colors :accent)
                      :height           54
                      :flex-direction   "row"
                      :align-items      "center"
                      :justify-content  "center"}
                     (dissoc style :font-size :font-weight))}
   (when icon
     icon)
   [text {:style {:color     (s/colors :white)
                  :font-size (or (:font-size style) 20)
                  :font-weight (or (:font-weight style) "normal")}}
    (string/upper-case title)]])

(defn bottom-sheet [{:keys [height close-action visible?]} component]
  [modal {:animation-type "slide"
          :transparent true
          :visible visible?
          :on-request-close close-action}
   [view {:style {:flex-direction "column"
                  :justify-content "space-between"
                  :height "100%"
                  :background-color "#00000050"}}
    [touchable-opacity
     {:on-press close-action
      :style {:flex 1}}]
    [view {:style {:width (:width dimensions)
                   :background-color (s/colors :window-backround)
                   :align-self "flex-end"
                   :height height
                   :border-radius 4}}
     component]]])

(defn floating-label [focused? has-text? label-text error]
  (let [aval (r/atom (new (.-Value Animated) 0))]
    (r/create-class
     {:component-did-mount
      (fn [] (reset! aval (new (.-Value Animated) 0)))

      :component-did-update
      (fn [this]
        (let [[_ focused? has-text? label-text]
              (:argv (js->clj (.-props this) :keywordize-keys true))
              active? (or focused? has-text?)]
          (.start (timing @aval
                          (clj->js {:toValue
                                    (cond (nil? active?)   0
                                          (true? active?)  1
                                          (false? active?) 0)
                                    :duration 80})))))

      :reagent-render
      (fn [focused? has-text? label-text error]
        [atext
         {:style {:position  "absolute"
                  :left      4
                  :top       (.interpolate
                              @aval
                              (clj->js {:inputRange  [0 1]
                                        :outputRange [24 0]}))
                  :font-size (.interpolate
                              @aval
                              (clj->js {:inputRange  [0 1]
                                        :outputRange [18 14]}))
                  :color     (cond (and focused?
                                        (some? error)) (s/colors :error)
                                   focused?            (s/colors :accent)
                                   :else               (s/colors :placeholder))}}
         label-text])})))

(defn input-error-byline [error]
  [text
   {:style {:font-size   12
            :margin-left 4
            :color       (s/colors :error)}}
   error])

(defn text-input-layout [props label-text]
  (let [id                  (str (random-uuid))
        state               (subscribe [:ui-text-input-layout id])
        animated-is-focused (r/atom nil)]
    (r/create-class
     {:component-did-mount
      (fn []
        (when-not (string/blank? (:default-value props)))
        (dispatch [:ui-text-input-layout id :text (:default-value props)]))
      :reagent-render
      (fn [props label-text]
        (let [empty?    (string/blank? (:text @state))
              focused?  (:focus @state)
              has-text? (not empty?)]
          [view
           {:style (merge {:flex 1} (:style props))}
           [floating-label focused? has-text? label-text (:error props)]
           [text-input
            (merge
             {:on-focus                #(do
                                          (dispatch [:ui-text-input-layout id :focus true])
                                          (when-let [on-focus (:on-focus props)]
                                            (on-focus %)))
              :on-blur                 #(do
                                          (dispatch [:ui-text-input-layout id :focus false])
                                          (when-let [on-blur (:on-blur props)]
                                            (on-blur %)))
              :on-change-text          #(do
                                          (dispatch [:ui-text-input-layout id :text %])
                                          (when-let [oct (:on-change-text props)]
                                            (oct %)))
              :style                   {:font-size  18
                                        :margin-top 14}
              :underline-color-android (cond (some? (:error props)) (s/colors :error)
                                             (:focus @state)        (s/colors :accent)
                                             :else                  (s/colors :border))}
             (dissoc props :error :style :on-change-text :on-focus :on-blur))]
           (when (some? (:error props))
             [input-error-byline (:error props)])]))})))
