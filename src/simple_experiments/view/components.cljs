(ns simple-experiments.view.components
  (:require [reagent.core :as r :refer [atom]]))

(def ReactNative (js/require "react-native"))
(def micon (-> "react-native-vector-icons/MaterialIcons"
               js/require
               .-default
               r/adapt-react-class))
(def miconx (-> "react-native-vector-icons/MaterialCommunityIcons"
                js/require
                .-default
                r/adapt-react-class))

(def text (r/adapt-react-class (.-Text ReactNative)))
(def view (r/adapt-react-class (.-View ReactNative)))
(def image (r/adapt-react-class (.-Image ReactNative)))
(def touchable-highlight (r/adapt-react-class (.-TouchableHighlight ReactNative)))
(def status-bar (r/adapt-react-class (.-StatusBar ReactNative)))

(def logo-img (js/require "./images/cljs.png"))

(defn alert [title]
  (.alert (.-Alert ReactNative) title))
