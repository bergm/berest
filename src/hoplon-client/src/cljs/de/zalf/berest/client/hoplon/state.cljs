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
(cell= (println "state: \n" (pr-str state)))

;cell holding immutable minimal crops from server, these won't change and thus
;doesn't constitute a real stem cell
(defc minimal-all-crops nil)
#_(cell= (println "minimal-all-crops:\n " (pr-str minimal-all-crops)))

;cell holding static app state, which will hardly change
(defc static-state nil)
#_(cell= (println "static-state:\n " (pr-str static-state)))

(defc= slopes (:slopes static-state))
(defc= stts (:stts static-state))
(defc= substrate-groups (into {} (map (juxt :soil.substrate/key identity) (:substrate-groups static-state))))
(defc= ka5-soil-types (into {} (map (juxt :soil.type.ka5/name identity) (:ka5-soil-types static-state))))
(defc= crop->dcs (:crop->dcs static-state))
#_(defc= stt-descriptions (into {} (map (juxt :soil.stt/key :soil.stt/description) stts)))

;local state
(defc weather-station-data {})
#_(cell= (println "weather-station-data: " (pr-str weather-station-data)))

;derived state

(defc= farms (:farms state))

(defc= user-weather-stations (:weather-stations state))
(cell= (println "user-weather-stations: " (pr-str user-weather-stations)))

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
  [result-cell weather-station-id years]
  ((mkremote 'de.zalf.berest.web.castra.api/get-weather-station-data
             result-cell error loading) weather-station-id years))

(def load-minimal-all-crops
  (mkremote 'de.zalf.berest.web.castra.api/get-minimal-all-crops minimal-all-crops error loading))

(def load-static-state
  (mkremote 'de.zalf.berest.web.castra.api/get-static-state static-state error loading))

(defn load-crop-data
  [result-cell crop-id]
  ((mkremote 'de.zalf.berest.web.castra.api/get-crop-data
             result-cell error loading) crop-id))

(def create-new-farm (mkremote 'de.zalf.berest.web.castra.api/create-new-farm state error loading))
(def create-new-plot (mkremote 'de.zalf.berest.web.castra.api/create-new-plot state error loading))

(def create-new-farm-address (mkremote 'de.zalf.berest.web.castra.api/create-new-farm-address state error loading))

(def create-new-soil-data-layer (mkremote 'de.zalf.berest.web.castra.api/create-new-soil-data-layer state error loading))
(def set-substrate-group-fcs-and-pwps (mkremote 'de.zalf.berest.web.castra.api/set-substrate-group-fcs-and-pwps state error loading))

(def create-new-donation (mkremote 'de.zalf.berest.web.castra.api/create-new-donation state error loading))
(def create-new-crop-instance (mkremote 'de.zalf.berest.web.castra.api/create-new-crop-instance state error loading))
(def create-new-dc-assertion (mkremote 'de.zalf.berest.web.castra.api/create-new-dc-assertion state error loading))

(def update-db-entity (mkremote 'de.zalf.berest.web.castra.api/update-db-entity state error loading))

(def delete-db-entity (mkremote 'de.zalf.berest.web.castra.api/delete-db-entity state error loading))
(def delete-db-entities (mkremote 'de.zalf.berest.web.castra.api/delete-db-entity state error loading))


;TODO: can't update weather-stations easily as they're actually shared most of the time, do this later properly
#_(def update-weather-station (mkremote 'de.zalf.berest.web.castra.api/update-weather-station state error loading))


(defn init-after-login []
  (when-not @minimal-all-crops (load-minimal-all-crops))
  #_(get-state)
  #_(js/setInterval #(if @logged-in? (get-state)) 100))







