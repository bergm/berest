(ns berest.queries
  (:require [clojure.string :as cs]
            [clojure.edn :as edn]
            [datomic.api :as d]
            [berest.datomic :as db]))

(defn get-entities [db id-attr]
  (let [result (d/q '[:find ?e
                      :in $ ?id-attr
                      :where
                      [?e ?id-attr]]
                    db id-attr)]
    (->> result
         (map first ,,,)
         (map (partial d/entity db) ,,,))))
