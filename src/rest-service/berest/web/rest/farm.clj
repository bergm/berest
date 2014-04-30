(ns berest.web.rest.farm
  (:require [berest.core :as bc]
            [berest.datomic :as db]
            [berest.helper :as bh :refer [rcomp]]
            [berest.web.rest.common :as common]
            [berest.web.rest.queries :as queries]
            [berest.web.rest.util :as util]
            [berest.web.rest.template :as temp]
            [datomic.api :as d]
            [ring.util.response :as rur]
            [hiccup.element :as he]
            [hiccup.def :as hd]
            [hiccup.form :as hf]
            [hiccup.page :as hp]
            [clojure.edn :as edn]))


(defn vocab
  "translatable vocabulary for this page"
  [element & [lang]]
  (get-in {:farms {:lang/de "Betriebe"
                   :lang/en "farms"}
           :show {:lang/de "Hier werden alle in der Datenbank
                  gespeicherten Betriebe angezeigt."
                  :lang/en "Here will be displayed all farms
                  stored in the database."}
           :create {:lang/de "Neuen Betrieb erstellen:"
                    :lang/en "Create new farm:"}
           :create-button {:lang/de "Erstellen"
                           :lang/en "Create"}

           }
          [element (or lang common/*lang*)] "UNKNOWN element"))


(defn create-farm
  [request]
  "post to create a new farm")

(defn get-farm
  [id request]
  (str "Farm no: " id " and full request: " request))

(defn update-farm
  [id request]
  (str "put to farm id: " id " and full request: " request))















