(ns ticker.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [cljsjs.juration])
  (:import [goog.date Date DateTime DateRange Interval]))

;; -------------------------
;; Views

(def expectations {:male (Interval. 77 9 0 0 0 0)
                   :female (Interval. 82 10 0 0 0 0)})

(defn format-time-left [{:keys [now birthday gender]}]
  (let [expectation (get expectations gender)
        death (doto (.clone birthday) (.add expectation))]
    (js/juration.stringify (->
                            (- (.valueOf death) (.valueOf now))
                            (/ 1000)) #js {:format "long"})))

(defn home-page []
  (let [settings (atom {:gender :male
                        :now (DateTime.)
                        :birthday (DateTime. 1982 8 30)})
        tick (fn ticker []
               (swap! settings assoc :now (DateTime.))
               (js/setTimeout ticker 1000))]
    (tick)
    (fn []      
      [:div [:h2 "Welcome to ticker"]
       [:div.settings [:h3 "Settings"]]
       [:div.clock
        [:div
         (str "You as a " (name (:gender @settings)) " born on " (:birthday @settings)
              " have that much time left:"  )
         [:br]
         [:span.time-left (format-time-left @settings)]]]])))

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))


;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
   {:nav-handler
    (fn [path]
      (secretary/dispatch! path))
    :path-exists?
    (fn [path]
      (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
