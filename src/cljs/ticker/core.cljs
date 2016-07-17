(ns ticker.core
  (:require [reagent.core :as reagent :refer [atom]]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [accountant.core :as accountant]
            [attic.core :as attic]
            [goog.date]
            [cljsjs.juration])
  (:import [goog.date DateTime Interval Date]
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

(defn get-inital-state []
  (let [from-local-storage (attic/get-item :ticker/settings)
        from-local-storage (merge from-local-storage {:birthday (Date. (js/Date. (:birthday from-local-storage)))})]
    (println from-local-storage)
    (merge {:gender :male
            :now (DateTime.)
            :birthday (DateTime.)} from-local-storage)))

(defn dates-as-nums [settings]
  (-> settings
      (dissoc :now)
      (update :birthday #(.valueOf %))))

(defn home-page []
  (let [settings (atom (get-inital-state))
        store-settings! (fn []
                          (->> @settings dates-as-nums (attic/set-item :ticker/settings)))]
    (fn []
      (js/setTimeout #(swap! settings assoc :now (DateTime.)) 1000)

      [:div [:h2 "Welcome to ticker"]
       [:div.settings [:h3 "Settings"]
        (let [change-gender (fn [e]
                              (swap! settings assoc :gender (-> e .-target .-value keyword))
                              (store-settings!))]
          [:form
           
           [:label {:for "gender"} "Are you: "]
           
           [:input {:type "radio" :name "gender" :value "male" :defaultChecked (= :male
                                                                                  (:gender @settings))
                    :on-change change-gender}] "Male"
           [:input {:type "radio" :name "gender" :value "female" :defaultChecked (= :female
                                                                                    (:gender @settings))
                    :on-change change-gender}] "Female"
           [:br]
           [:label {:for "birthday"} "Enter your date of birth: "]
           [:input {:type "date" :name "birthday" :value (-> (:birthday @settings)
                                                             (format-date :format "yyyy-MM-dd"))
                    :on-change (fn [e]
                                 (let [date (-> e .-target .-value goog.date/fromIsoString)]
                                   (swap! settings assoc :birthday date)
                                   (store-settings!)))}]])]
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
