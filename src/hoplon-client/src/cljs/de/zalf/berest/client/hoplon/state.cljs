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

;local state stem cell
(defc local-state {:bersim {:selected-farm-id nil
                            :selected-plot-id nil}})
(cell= (println "local-state: \n" (pr-str local-state)))

;derived state

(defc= donations (:donations state))

(defc temp-donations {:day nil :month nil :amount nil})

(defc= farms (:farms state))
(defc= selected-farm (if-let [sf-id (some-> local-state :bersim :selected-farm-id)]
                       (sf-id farms)
                       (-> farms first second)))

(defc= plots (:plots selected-farm))
(defc= selected-plot (if-let [sp-id (some-> local-state :bersim :selected-plot-id)]
                       (sp-id plots)
                       (-> plots first second)))

(defc= until-date (:until-date state))


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

(defn init []
  (get-state)
  #_(js/setInterval #(if @logged-in? (get-state)) 100))







