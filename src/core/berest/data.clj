(ns berest.data
  (:require [berest.core :as bc]
            [berest.datomic :as db]
            [berest.helper :as bh :refer [rcomp]]
            [datomic.api :as d]))


(defn db->users
  [db & [full-url]]
  (->> (d/q '[:find ?user-e
              :in $
              :where
              [?user-e :user/id]]
            db)
       (map (rcomp first (partial d/entity db)) ,,,)
       (map #(select-keys % [:user/id :user/full-name]) ,,,)
       (map #(assoc % :url (str full-url (:user/id %) "/")) ,,,)))

(defn db->farms
  [db user-id & [full-url]]
  (->> (d/q '[:find ?farm-e
              :in $ ?user-id
              :where
              [?user-e :user/id ?user-id]
              [?user-e :user/farms ?farm-e]]
            db user-id)
       (map (rcomp first (partial d/entity db)) ,,,)
       (map #(select-keys % [:farm/id :farm/name]) ,,,)
       (map #(assoc % :url (str full-url (:farm/id %) "/")) ,,,)))

(defn db->plots
  [db farm-id & [full-url]]
  (->> (d/q '[:find ?plot-e
              :in $ ?user-id ?farm-id
              :where
              [?farm-e :farm/id ?farm-id]
              [?farm-e :farm/plots ?plot-e]]
            db farm-id)
       (map (rcomp first (partial d/entity db)),,,)
       (map #(select-keys % [:plot/id :plot/name]),,,)))


