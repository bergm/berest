(ns berest.web.castra.api
  (:require [tailrecursion.castra :refer [defrpc  ex error *session*]]
            [berest.web.castra.rules :as rules]
            [berest.data :as data]
            [berest.util :as util]
            [berest.api :as api]
            [berest.datomic :as db]
            [datomic.api :as d]
            [simple-time.core :as time]
            [clj-time.core :as ctc]
            [clj-time.format :as ctf]
            [clj-time.coerce :as ctcoe]
            [clojure-csv.core :as csv]))


;;; utility ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn call      [f & args]  (apply f args))
(defn apply-all [fns coll]  (mapv #(%1 %2) (cycle fns) coll))
(defn every-kv? [fn-map m]  (->> m (merge-with call fn-map) vals (every? identity)))
(defn map-kv    [kfn vfn m] (into (empty m) (map (fn [[k v]] [(kfn k) (vfn v)]) m)))
(defn map-k     [kfn m]     (map-kv kfn identity m))
(defn map-v     [vfn m]     (map-kv identity vfn m))

;;; internal ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

#_(defn new-message [db-val from conv text]
  {:from from, :conv conv, :text text})

#_(defn add-message [db-val from conv text]
  (let [cons* #(cons %2 (or %1 '()))]
    (update-in db-val [:messages conv] cons* (new-message db-val from conv text))))

(defn get-farms
  [db user-id]
  (map #(select-keys % [:farm/id :farm/name]) (data/db->farms db user-id)))

(defn get-plots
  [db farm-id]
  (map #(select-keys % [:plot/id :plot/name]) (data/db->plots db farm-id)))

;;; public ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrpc get-berest-state [& [user-id pwd]]
  {:rpc/pre [(nil? user-id)
             (rules/logged-in?)]}
  (let [db (db/current-db)
        cred (if user-id
               (db/credentials* db user-id pwd)
               (:user @*session*))

        farms-with-plots
        (into {} (map (fn [{farm-id :farm/id
                            :as farm}]
                        [farm-id (assoc farm :plots (into {} (map (juxt :plot/id identity)
                                                                  (get-plots db farm-id))))])
                      (get-farms db (:user/id cred))))]
    (when cred
      {:farms farms-with-plots
       :selected-farm-id (-> farms-with-plots first first)
       :selected-plot-id (-> farms-with-plots first second :plots first first)
       :until-date #inst "1993-09-30"
       :donations [{:day 1 :month 4 :amount 22}
                   {:day 2 :month 5 :amount 10}
                   {:day 11 :month 7 :amount 30}]
       :user cred})))

#_(defrpc register [user pass1 pass2]
  {:rpc/pre [(register! db user pass1 pass2)]}
        (get-state user))

(defrpc login
  [user-id pwd]
  {:rpc/pre [(rules/login! user-id pwd)]}
  (get-berest-state))

(defrpc logout
  []
  {:rpc/pre [(rules/logout!)]}
  nil)

(defn calc-or-sim-csv
  [f plot-id until-date donations]
  (let [db (db/current-db)
        ud (ctcoe/from-date until-date)
        until-julian-day (.getDayOfYear ud)
        year (ctc/year ud)
        donations (for [{:keys [day month amount]} donations]
                    {:donation/abs-day (util/date-to-doy day month year)
                     :donation/amount amount})
        {:keys [inputs soil-moistures]} (f db plot-id until-julian-day year donations [])]
    (->> soil-moistures
         (api/create-csv-output inputs ,,,)
         (#(csv/write-csv % :delimiter \tab) ,,,))))

(defrpc simulate-csv
  [plot-id until-date donations]
  {:rpc/pre [(rules/logged-in?)]}
  (calc-or-sim-csv api/simulate-plot-from-db plot-id until-date donations))

(defrpc calculate-csv
  [plot-id until-date donations]
  {:rpc/pre [(rules/logged-in?)]}
  (calc-or-sim-csv api/calculate-plot-from-db plot-id until-date donations))
