(ns berest.web.rest.data
  (:require [clojure.string :as cs]
            [datomic.api :as d]
            [ring.util.response :as rur]
            [hiccup.element :as he]
            [hiccup.def :as hd]
            [hiccup.form :as hf]
            [hiccup.page :as hp]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [clojure.tools.logging :as log]
            [berest.core :as bc]
            [berest.datomic :as db]
            [berest.web.rest.common :as common]
            [berest.web.rest.queries :as queries]
            [berest.web.rest.template :as temp]))

;; page translations

(defn vocab
  "translatable vocabulary for this page"
  [element & [lang]]
  (get-in {:wstations {:lang/de "Wetterstationen"
                       :lang/en "Weatherstations"}
           :farms {:lang/de "Landwirtschaftliche Betriebe"
                   :lang/en "Farms"}
           :show {:lang/de "Hier werden alle in der Datenbank
                  gespeicherten Wetterstationen angezeigt."
                  :lang/en "Here will be displayed all weather stations
                  stored in the database."}
           :create {:lang/de "Neue Wetterstation erstellen:"
                    :lang/en "Create new weather station:"}
           :create-button {:lang/de "Erstellen"
                           :lang/en "Create"}

           }
          [element (or lang common/*lang*)] "UNKNOWN element"))

;; page layouts

(defn data-layout [db url]
  [:div.container
   (temp/standard-get-post-h3 url)

   [:div [:a {:href (str url "weather-stations/")} (vocab :wstations)]]
   [:div [:a {:href (str url "farms/")} (vocab :farms)]]])

(defn get-data
  [request]
  (let [db (db/current-db)]
    (common/standard-get (partial data-layout db)
                         request)))

