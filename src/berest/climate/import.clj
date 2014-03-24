(ns berest.climate.import
  (:require [clojure.java.io :as cjio]
            [clojure.string :as str]
            [clj-time.core :as ctc]
            [clj-time.format :as ctf]
            [clj-time.coerce :as ctcoe]
            [berest.climate.algo :as algo]
            [berest.datomic :as db]
            [berest.util :as bu]
            [datomic.api :as d]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]
            [clojure-csv.core :as csv]))

(defn- read-and-parse-as-csv
  [file]
  (-> file
      cjio/resource
      slurp
      (csv/parse-csv :delimiter \,)))

(def climate-data-zalf-1992-1998
  #(read-and-parse-as-csv "private/climate/climate-data-muencheberg-zalf-1992-to-1998.csv"))

(def climate-data-muencheberg-1993-1998
  #(read-and-parse-as-csv "private/climate/climate-data-muencheberg-1993-to-1998.csv"))

(defn muencheberg-csv-data->transaction-weather-data
  [climate-data]
  (map (fn [line]
         (let [[tavg precip globrad] (map #(Double/parseDouble %) (drop 3 line))
               date (ctcoe/to-date ((fn [[d m y]] (str y "-" m "-" d)) (take 3 line)))]
           {:weather-data/prognosis-data? false
            :weather-data/date date
            :weather-data/precipitation precip
            :weather-data/evaporation (algo/potential-evaporation-turc-wendling globrad tavg)
            :weather-data/average-temperature tavg
            :weather-data/global-radiation globrad}))
       (rest climate-data)))

(defn transact-data
  [db-connection station-t-data weather-t-data]
  (let [s-t-data (merge {:db/id (d/tempid :db.part/user)} station-t-data)
        t-data (assoc s-t-data :weather-station/data weather-t-data)]
    (try
      #_(println "t-data: " [t-data])
      (d/transact db-connection [t-data])
      (catch Exception e
        (log/info "Couldn't transact weather data to datomic! data: [\n" t-data "\n]")
        (throw e)))))

(defn transact-zalf-data
  [db-connection]
  (let [station-to-data {{:weather-station/id "zalf/zalf"
                          :weather-station/name "ZALF Wetterstation"} (climate-data-zalf-1992-1998)
                         {:weather-station/id "zalf/muencheberg"
                          :weather-station/name "DWD Station Müncheberg"} (climate-data-muencheberg-1993-1998)}]
    (->> (vals station-to-data)
         (map muencheberg-csv-data->transaction-weather-data ,,,)
         (map (partial transact-data db-connection) (keys station-to-data) ,,,))))


(comment "transact data to a defined db  "

  (transact-zalf-data (db/connection db/climate-db-id))

  (muencheberg-csv-data->transaction-weather-data (climate-data-muencheberg-1993-1998))

  )
