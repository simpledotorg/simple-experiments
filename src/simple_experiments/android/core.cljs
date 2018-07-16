(ns simple-experiments.android.core
  (:require [simple-experiments.view :as view]
            [simple-experiments.store :as store]
            [reagent.core :as r :refer [atom]]
            [re-frame.core :refer [subscribe dispatch dispatch-sync]]))

(def ReactNative (js/require "react-native"))

(def app-registry (.-AppRegistry ReactNative))

(defn app-root []
  [view/app-root])

(defn init []
  (dispatch-sync [:initialize-db])
  (store/init!)
  (.registerComponent app-registry "SimpleExperiments" #(r/reactify-component app-root)))
