(ns de.zalf.berest.client.hoplon.bersim.state
  (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]])
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [tailrecursion.javelin :as j :refer [cell]]
            [tailrecursion.castra  :as c :refer [mkremote]]))

(enable-console-print!)

(defc state {})
#_(cell= (println "state: " (pr-str state)))
#_{:crops crops
 :weather {} #_{:weather-data/prognosis-data? true
                    :weather-data/date date*
                    :weather-data/precipitation (parse-german-double rr-s)
                    :weather-data/evaporation (parse-german-double vp-t)
                    :weather-data/average-temperature (parse-german-double tm)
                    :weather-data/global-radiation (parse-german-double gs)}
 :selected-crop-id (-> crops first :crop/id)
 :until-date #inst "1993-09-30"
 :donations [{:day 1 :month 4 :amount 22}
             {:day 2 :month 5 :amount 10}
             {:day 11 :month 7 :amount 30}]
 :technology {:technology/cycle-days 1
              :technology/outlet-height 200
              :technology/sprinkle-loss-factor 0.4
              :technology/type :technology.type/drip ;:technology.type/sprinkler
              :donation/min 1
              :donation/max 30
              :donation/opt 20
              :donation/step-size 5}
 :plot {:plot/stt 6212
        :plot/slope 1
        :plot/field-capacities []
        :plot/fc-unit :soil-moisture.unit/volP
        :plot/permanent-wilting-points []
        :plot/pwp-unit :soil-moisture.unit/volP
        :plot/ka5-soil-types []
        :plot/groundwaterlevel 300}
 :user cred}

(defc= technology-cycle-days (-> state :technology :technology/cycle-days))
(defn set-technology-cycle-days
  [value]
  (swap! state update-in [:technology :technology/cycle-days] value))

(defc= technology-outlet-height (-> state :technology :technology/outlet-height))
(defn set-technology-cycle-days
  [value]
  (swap! state update-in [:technology :technology/cycle-days] value))



(defc error nil)
(defc loading [])

(defc csv-result nil)
(cell= (println "csv-result: " (pr-str csv-result)))
(defc calc-error nil)
(defc calculating [])

(defc= loaded?      (not= {} state))
(defc= loading?     (seq loading))
(defc= logged-in?   (not (or (nil? state) (= {} state))))
(defc= show-login?  (and loaded? (not logged-in?)))

(def clear-error!   #(reset! error nil))

(def login! (mkremote 'berest.web.castra.api/login state error loading))
(def logout! (mkremote 'berest.web.castra.api/logout state error loading))
(def get-state (mkremote 'berest.web.castra.api/get-bersim-state state error loading))
(def calculate-csv (mkremote 'berest.web.castra.api/calculate-csv csv-result calc-error calculating))
(def simulate-csv (mkremote 'berest.web.castra.api/simulate-csv csv-result calc-error calculating))

(defn init []
  (get-state)
  #_(js/setInterval #(if @logged-in? (get-state)) 100))






(comment

  (defc= user         (:user state))
  (defc= buddies      (:users state))
  (defc= convs        (sort (keys (:messages state))))
  (defc= msgs         (get-in state [:messages active-chat]))
  (defc= loop-convs   (mapv (fn [x] [x, (str/join ", " (disj x user))]) convs))


  (def switch-chat!   #(reset! active-chat @%))
  (def toggle-chat*   #(cond (contains? %1 %2) (disj %1 %2) %1 (conj %1 %2) :_ #{@user %2}))
  (def toggle-chat!   #(swap! active-chat toggle-chat* @%))


  (def send-message*  (mkremote 'berest-client.castra-api/send-message   state error loading))
  (def send-message!  #(send-message* @user @active-chat %))

  (cell=
    (let [s (get-in error [:data :state] ::nope)]
      (if-not (= ::nope s) (reset! ~(cell state) s))))


  )


