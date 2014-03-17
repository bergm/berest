(ns berest.climate.climate
  (:require [clojure.java.io :as cjio]
            [clojure.string :as str]
            [clj-time.core :as ctc]
            [clj-time.format :as ctf]
            [clj-time.coerce :as ctcoe]
            [berest.climate.algo :as algo]
            [berest.datomic :as db]
            [berest.util :as bu]
            [berest.queries :as queries]
            [datomic.api :as d]
            [clojure.pprint :as pp]))

(defn weather-data
  [db weather-station-id year]
  (let [start-of-year-1 (ctcoe/to-date (ctc/date-time (dec year) 12 31))
        end-of-year+1 (ctcoe/to-date (ctc/date-time (inc year) 1 1))
        data (d/q '[:find ?ws-e-id ?data-e-id
                    :in $ ?ws-id ?soy-1 ?eoy+1
                    :where
                    [?ws-e-id :weather-station/id ?ws-id]
                    [?ws-e-id :weather-station/data ?data-e-id]
                    [?data-e-id :weather-data/date ?date]
                    [(.after ?date ?soy-1)]
                    [(.before ?date ?eoy+1)]]
                  db weather-station-id start-of-year-1 end-of-year+1)]
    {:station (d/entity db (ffirst data))
     :data (map #(d/entity db (second %)) data)}))

(defn sorted-weather-data-map
  [db weather-station-id year]
  (->> (weather-data db weather-station-id year)
       (map #(vector (bu/date-to-doy (:weather-data/date %)) %) ,,,)
       (into (sorted-map) ,,,)))


(comment "retract some stations climate data"

  (d/transact (db/datomic-connection "berest") [[:db.fn/retractEntity 17592186062454]
                                                [:db.fn/retractEntity 17592186067570]])

  )

(comment "find some stations"

  (def db (db/current-db "berest"))
  (def con (db/datomic-connection "berest"))
  (def stations (queries/get-entities db :weather-station/id))
  (filter #(.startsWith (:weather-station/id %) "zalf/") stations)

  (d/transact con [{:db/id [:weather-station/id "de.zalf/zalf"]
                    :weather-station/id "zalf/zalf"}])

  )


(comment "check weather-data of a station"

  (def t (weather-data (db/current-db "berest") "zalf/zalf" 1993))
  (sort-by :weather-data/date (map d/touch (:data t)))
  (count (:data t))

  )


(defn longterm-evap-precip [doy]
  (let [longterm-average-evaporation-values
        [#_"01.04." 1.1, 1.2, 1.2, 1.2, 1.3, 1.3, 1.3, 1.4, 1.4, 1.4,
         #_"11.04." 1.4, 1.5, 1.5, 1.5, 1.6, 1.6, 1.6, 1.7, 1.7, 1.7,
         #_"21.04." 1.8, 1.8, 1.8, 1.9, 1.9, 1.9, 2.0, 2.0, 2.0, 2.1,
         #_"01.05." 2.1, 2.1, 2.2, 2.2, 2.2, 2.3, 2.3, 2.3, 2.4, 2.4,
         #_"11.05." 2.4, 2.5, 2.5, 2.5, 2.6, 2.6, 2.6, 2.7, 2.7, 2.7,
         #_"21.05." 2.7, 2.8, 2.8, 2.8, 2.9, 2.9, 2.9, 2.9, 3.0, 3.0, 3.0,
         #_"01.06." 3.0, 3.1, 3.1, 3.1, 3.2, 3.2, 3.2, 3.2, 3.3, 3.3,
         #_"11.06." 3.3, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4,
         #_"21.06." 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4, 3.4,
         #_"01.07." 3.4, 3.4, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3,
         #_"11.07." 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.3, 3.2, 3.2, 3.2,
         #_"21.07." 3.2, 3.2, 3.2, 3.1, 3.1, 3.1, 3.1, 3.1, 3.1, 3.1, 3.0,
         #_"01.08." 3.0, 3.0, 3.0, 3.0, 3.0, 2.9, 2.9, 2.9, 2.9, 2.9,
         #_"11.08." 2.9, 2.9, 2.8, 2.8, 2.8, 2.8, 2.7, 2.7, 2.7, 2.7,
         #_"21.08." 2.6, 2.6, 2.6, 2.6, 2.5, 2.5, 2.4, 2.4, 2.4, 2.4, 2.4,
         #_"01.09." 2.3, 2.3, 2.3, 2.2, 2.2, 2.2, 2.2, 2.1, 2.1, 2.1,
         #_"11.09." 2.0, 2.0, 2.0, 2.0, 1.9, 1.9, 1.9, 1.8, 1.8, 1.8,
         #_"21.09." 1.7, 1.7, 1.6, 1.6, 1.6, 1.5, 1.5, 1.5, 1.4, 1.4,
         #_"01.10." 1.4, 1.3, 1.3, 1.3, 1.2, 1.2, 1.2, 1.1, 1.1, 1.0,
         #_"11.10." 1.0, 1.0, 0.9, 0.9, 0.9, 0.7, 0.6, 0.7, 0.5, 0.8,
         #_"21.10." 1.0, 1.0, 0.9, 0.9, 0.9, 0.7, 0.6, 0.7, 0.5, 0.8]
        longterm-average-precipitation-values
        [#_"01.04." 1.0, 1.7, 0.6, 0.5, 1.1, 0.9, 0.9, 0.9, 1.9, 1.5,
         #_"11.04." 1.1, 0.8, 1.2, 1.5, 2.2, 0.9, 1.4, 1.1, 2.0, 0.9,
         #_"21.04." 0.7, 1.3, 0.9, 0.9, 0.4, 0.6, 0.9, 1.0, 2.0, 1.6,
         #_"01.05." 1.3, 1.1, 2.0, 1.5, 1.6, 1.8, 1.9, 1.3, 1.0, 1.3,
         #_"11.05." 4.2, 0.6, 1.6, 1.5, 1.3, 0.6, 0.9, 1.9, 1.4, 4.6,
         #_"21.05." 1.0, 0.9, 0.4, 0.9, 2.7, 1.0, 3.6, 2.8, 0.7, 2.2, 2.3,
         #_"01.06." 1.1, 2.2, 0.6, 1.3, 1.0, 0.8, 0.7, 2.7, 4.4, 3.5,
         #_"11.06." 2.0, 6.0, 1.3, 1.0, 1.8, 1.9, 1.5, 1.0, 3.3, 1.5,
         #_"21.06." 1.9, 2.8, 0.7, 0.6, 3.6, 2.4, 4.1, 3.3, 3.5, 1.9,
         #_"01.07." 1.6, 1.5, 1.9, 3.0, 3.4, 1.9, 1.1, 0.9, 2.5, 1.2,
         #_"11.07." 1.3, 2.2, 1.5, 1.0, 2.5, 2.0, 1.9, 3.4, 1.1, 4.3,
         #_"21.07." 3.6, 3.7, 3.6, 1.5, 0.9, 1.4, 2.1, 1.0, 1.4, 1.2, 0.9,
         #_"01.08." 0.8, 0.5, 2.4, 1.7, 1.0, 1.3, 0.8, 1.7, 1.9, 1.3,
         #_"11.08." 2.0, 1.7, 1.4, 0.3, 2.3, 1.7, 2.8, 1.1, 1.1, 3.1,
         #_"21.08." 1.6, 2.9, 1.2, 1.4, 2.6, 1.4, 2.4, 3.2, 4.0, 1.6, 0.6,
         #_"01.09." 2.1, 0.5, 0.3, 1.0, 1.4, 1.6, 3.1, 1.8, 2.6, 2.3,
         #_"11.09." 2.9, 1.0, 1.2, 1.9, 0.6, 2.0, 1.8, 1.1, 0.7, 1.2,
         #_"21.09." 0.6, 1.5, 0.6, 2.3, 1.2, 0.9, 0.6, 2.2, 2.3, 1.0,
         #_"01.10." 0.6, 0.8, 2.5, 0.4, 0.7, 0.5, 2.1, 0.5, 1.1, 2.4,
         #_"11.10." 0.8, 0.2, 0.9, 1.6, 1.0, 2.5, 1.7, 1.6, 1.5, 1.0,
         #_"21.10." 2.2, 2.9, 1.8, 1.4, 1.2, 0.6, 1.3, 2.0, 0.4, 1.9]]
    (if (and (< 90 doy) (<= doy (+ 90 213)))
      (let [index (- doy 90 1)]
        {:tavg -9999
         :globrad -9999
         :evaporation (nth longterm-average-evaporation-values index),
         :precipitation (nth longterm-average-precipitation-values index)})
      {:tavg -9999
       :globrad -9999
       :evaporation -9999
       :precipitation -9999})))

(defn weather-at [m doy]
  (if-let [v (find m doy)]
    (second v)
    (assoc (longterm-evap-precip doy) :prognosis? true)))
