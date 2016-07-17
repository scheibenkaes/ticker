(ns ticker.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [goog.date]
            [cljsjs.juration])
  (:import [goog.date DateTime Interval]
           [goog.i18n DateTimeFormat]))

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

(defn format-date [date & {:keys [format] :or {format "MM/dd/y"}}]
  (-> (DateTimeFormat. format) (.format date)))

(defn home-page []
  (let [settings (atom {:gender :male
                        :now (DateTime.)
                        :birthday (DateTime.)})]
    (fn []
      (js/setTimeout #(swap! settings assoc :now (DateTime.)) 1000)

      [:div [:h2 "Welcome to ticker"]
       [:div.settings [:h3 "Settings"]
        (let [change-gender (fn [e]
                               (swap! settings assoc :gender (-> e .-target .-value keyword)))]
          [:form
           
           [:label {:for "gender"} "Select a gender: "]
           
           [:input {:type "radio" :name "gender" :value "male" :defaultChecked (= :male
                                                                                  (:gender @settings))
                    :on-change change-gender}] "Male"
           [:input {:type "radio" :name "gender" :value "female" :defaultChecked (= :female
                                                                                    (:gender @settings))
                    :on-change change-gender}] "Female"
           [:br]
           [:label {:for "birthday"} "Enter your birthday: "]
           [:input {:type "date" :name "birthday" :value (-> (:birthday @settings)
                                                             (format-date :format "yyyy-MM-dd"))
                    :on-change (fn [e]
                                 (let [date (-> e .-target .-value goog.date/fromIsoString)]
                                   (swap! settings assoc :birthday date)))}]])]
       [:div.clock
        [:h3 "Timer"]
        [:div
         (str "As a " (name (:gender @settings)) " born on " (-> (:birthday @settings) format-date)
              " your time runs out in:"  )
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
