(ns berest.api
  (:require [clojure.string :as cs]
            [datomic.api :as d]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [berest.core :as bc]
            [berest.plot :as plot]
            [berest.datomic :as db]
            [berest.util :as bu]
            [berest.helper :as bh :refer [rcomp]]
            [berest.climate.climate :as climate]
            [simple-time.core :as time]))

(defn calculate-plot-from-db
  [db plot-id until-julian-day year donations dc-assertions]
  (when-let [plot (bc/db-read-plot db plot-id year)]
    (let [;plot could be updated with given dc-assertions
          ;plot* (update-in plot [])

           sorted-weather-map (climate/final-sorted-weather-data-map-for-plot db year plot-id)

           inputs (bc/create-input-seq plot sorted-weather-map (+ until-julian-day 7)
                                       donations :technology.type/sprinkler)
           inputs-7 (drop-last 7 inputs)

          ;xxx (map (rcomp (juxt :abs-day :irrigation-amount) str) inputs-7)
          ;_ (println xxx)

           prognosis-inputs (take-last 7 inputs)
           days (range (-> inputs first :abs-day) (+ until-julian-day 7 1))

           sms-7* (bc/calc-soil-moistures* inputs-7 (:plot.annual/initial-soil-moistures plot))
           _ (println sms-7*)
           {soil-moistures-7 :soil-moistures
            :as sms-7} (last sms-7*)
          #_(bc/calc-soil-moistures inputs-7 (:plot.annual/initial-soil-moistures plot))

           prognosis* (bc/calc-soil-moisture-prognosis* 7 prognosis-inputs soil-moistures-7)
           prognosis (last prognosis*)
          #_(bc/calc-soil-moisture-prognosis 7 prognosis-inputs soil-moistures-7)]
      {:inputs inputs
       :soil-moistures-7 (rest sms-7*)
       :prognosis prognosis*})))
