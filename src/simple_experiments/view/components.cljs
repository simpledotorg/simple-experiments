(ns simple-experiments.view.components
  (:require [reagent.core :as r :refer [atom]]
            [simple-experiments.view.styles :as s]
            [clojure.string :as string]))

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

(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def scroll-view (r/adapt-react-class (.-ScrollView ReactNative)))
(def button (r/adapt-react-class (.-Button ReactNative)))
(def text-input (r/adapt-react-class (.-TextInput ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def touchable-opacity (r/adapt-react-class (.-TouchableOpacity ReactNative)))
(def status-bar (r/adapt-react-class (.-StatusBar ReactNative)))
(def back-handler (.-BackHandler ReactNative))
(def scan-illustration (js/require "./images/scan_illustration.png"))

(defn alert [title]
  (.alert (.-Alert ReactNative) title))

(defn screen [display-name component on-back]
  (let [on-back (fn [] (on-back back-handler)
                  true)]
    (r/create-class
     {:display-name "home"
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
      :reagent-render component})))

(defn search-bar [input-properties]
  [view {:style {:flex-direction     "row"
                 :align-items        "center"
                 :shadow-offset      {:width 10 :height 10}
                 :shadow-color       "black"
                 :shadow-opacity     1.0
                 :padding-horizontal 10
                 :padding-vertical   5
                 :border-width       1
                 :border-color       "transparent"
                 :elevation          1
                 :margin-top         20}}
   [micon {:name  "search" :size 30
           :style {:margin-right 5}}]
   [text-input
    (merge {:placeholder             "Enter patient's name or phone"
            :placeholder-text-color  (s/colors :placeholder)
            :underline-color-android "transparent"
            :style                   {:flex      1
                                      :font-size 18}}
           input-properties)]])

(defn action-button [icon-name icon-family title action]
  (let [icon (case icon-family
               :regular micon
               :community miconx)]
    [touchable-opacity
     {:on-press action
      :style {:margin-top 20
              :background-color (s/colors :accent)
              :border-radius 4
              :elevation 1
              :height 48
              :flex-direction "row"
              :align-items "center"
              :justify-content "center"}}
     [icon {:name icon-name
            :size 26
            :color (s/colors :white)
            :style {:margin-right 10}}]
     [text {:style {:color (s/colors :white)
                    :font-size 16
                    :font-weight "400"}}
      (string/upper-case title)]]))
