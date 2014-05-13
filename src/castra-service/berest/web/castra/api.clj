(ns berest.web.castra.api
  (:require [tailrecursion.castra :refer [defrpc  ex error *session*]]
            [berest.web.castra.rules :as rules]
            [berest.data :as data]
            [berest.datomic :as db]
            [datomic.api :as d]))

(def counter (atom 0))

(defrpc get-state []
  {:rpc/query [{:random (rand-int 100)
                :counter @counter}]}
  (swap! counter inc))


;;; utility ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn call      [f & args]  (apply f args))
(defn apply-all [fns coll]  (mapv #(%1 %2) (cycle fns) coll))
(defn every-kv? [fn-map m]  (->> m (merge-with call fn-map) vals (every? identity)))
(defn map-kv    [kfn vfn m] (into (empty m) (map (fn [[k v]] [(kfn k) (vfn v)]) m)))
(defn map-k     [kfn m]     (map-kv kfn identity m))
(defn map-v     [vfn m]     (map-kv identity vfn m))

;;; internal ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn new-message [db-val from conv text]
  {:from from, :conv conv, :text text})

(defn add-message [db-val from conv text]
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
       :selected-plot-id (-> farms-with-plots first second :plots first)
       :until-date #inst "1993-09-30"
       :irrigation-data [[1 4 22] [2 5 10] [11 7 30]]
       :user cred})))

#_(defrpc register [user pass1 pass2]
  {:rpc/pre [(register! db user pass1 pass2)]}
        (get-state user))

(defrpc test []
  {:test 1})

(defrpc login
  [user-id pwd]
  {:rpc/pre [(rules/login! user-id pwd)]}
  (println "login in with user-id: " user-id " pwd: " pwd)
  (get-berest-state))

(defrpc logout
  []
  {:rpc/pre [(rules/logout!)]}
  nil)

#_(defrpc send-message [from conv text]
        {:rpc/pre [(logged-in?)]}
        (swap! db add-message from conv text)
        (get-state from))

