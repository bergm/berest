(ns de.zalf.berest.client.hoplon.state
  (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]])
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [tailrecursion.javelin :as j :refer [cell]]
            [tailrecursion.castra  :as c :refer [mkremote]]
            [tailrecursion.hoplon :as h]))

(enable-console-print!)

;stem cell
(defc state {})
#_(cell= (println "state: \n" (pr-str state)))


;local state
(defc weather-station-data {})
(cell= (println "weather-station-data: " (pr-str weather-station-data)))

;derived state

(defc= farms (:farms state))

(defc= technology-cycle-days (-> state :technology :technology/cycle-days))
(defn set-technology-cycle-days
  [value]
  (swap! state update-in [:technology :technology/cycle-days] value))

(defc= technology-outlet-height (-> state :technology :technology/outlet-height))
(defn set-technology-cycle-days
  [value]
  (swap! state update-in [:technology :technology/cycle-days] value))


(def route (h/route-cell "#/"))

(defc error nil)
(defc loading [])

(def clear-error!   #(reset! error nil))

(defc csv-result nil)
(cell= (println "csv-result: " (pr-str csv-result)))
(defc calc-error nil)
(defc calculating [])

(defc= user (:user-credentials state))

(defc= lang (:language state))
(cell= (println "lang: " (pr-str lang)))

(defc= loaded?      (not= {} state))
(defc= loading?     (seq loading))

(defc= logged-in?   (not (nil? user)))
(cell= (println "logged-in?: "(pr-str logged-in?)))

(defc= show-login?  (and #_loaded? (not logged-in?)))
(cell= (println "show-login?: " show-login?))

(defc= show-content?  (and loaded? logged-in?))


(def clear-error!   #(reset! error nil))

(def login! (mkremote 'de.zalf.berest.web.castra.api/login state error loading))
(def logout! (mkremote 'de.zalf.berest.web.castra.api/logout state error loading))
(def get-state (mkremote 'de.zalf.berest.web.castra.api/get-berest-state state error loading))
(def get-full-selected-crops (mkremote 'de.zalf.berest.web.castra.api/get-state-with-full-selected-crops state error loading))
(def calculate-csv (mkremote 'de.zalf.berest.web.castra.api/calculate-csv csv-result calc-error calculating))
(def simulate-csv (mkremote 'de.zalf.berest.web.castra.api/simulate-csv csv-result calc-error calculating))

(defn load-weather-station-data
  [weather-station-id]
  (let [result (cell nil)
        _ (cell= (swap! ~(cell weather-station-data) assoc weather-station-id result))]
    ((mkremote 'de.zalf.berest.web.castra.api/get-weather-station-data result error loading) weather-station-id)))

(defn init []
  (get-state)
  #_(js/setInterval #(if @logged-in? (get-state)) 100))







