(ns simple-experiments.events.core
  (:require [re-frame.core :as re]
            [cljs-time.core :as time]
            [cljs-time.coerce :as timec]))

(defonce events (atom []))

(defn map->sig [m]
  (cond
    (some? (:full-name m))
    (select-keys m [:full-name])

    :else
    (str (keys m))))

(def monitor-event
  (re/->interceptor
   :id      :monitor-event
   :before
   (fn [context]
     (swap! events conj
            (let [[event & args]
                  (get-in context [:coeffects :event])]
              {:at (timec/to-long (time/now))
               :event event
               :args (map (fn [x] (if (map? x)
                                    (map->sig x)
                                    x))
                          args)}))
     context)))

(defn reg-event-db [id handler]
  (re/reg-event-db id [monitor-event] handler))

(defn reg-event-fx [id handler]
  (re/reg-event-fx id [monitor-event] handler))
