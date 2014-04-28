(ns berest.web.rest.crop
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
            [clojure.edn :as edn]
            [clojure.string :as str]))


(defn vocab
  "translatable vocabulary for this page"
  [element & [lang]]
  (get-in {:crops {:lang/de "Feldfrüchte"
                   :lang/en "crops"}
           :description {:lang/de "Hier werden alle in der Datenbank
                  gespeicherten Feldfrüchte angezeigt."
                         :lang/en "Here will be displayed all crops
                  stored in the database."}
           #_:create #_{:lang/de "Neuen Betrieb erstellen:"
                    :lang/en "Create new farm:"}
           #_:create-button #_{:lang/de "Erstellen"
                           :lang/en "Create"}
           }
          [element (or lang common/*lang*)] "UNKNOWN element"))


(defn create-crops-layout [db]
  [:div.container
   (for [e (queries/get-ui-entities db :rest.ui/groups :crop)]
     (common/create-form-element db e))

   #_[:button.btn.btn-primary {:type :submit} (vocab :create-button)]])

(defn all-crops
  [db url]
  (->> (d/q '[:find ?crop-e
              :in $
              :where
              [?crop-e :crop/id _]]
            db)
       (map (rcomp first (partial d/entity db)) ,,,)

       (map #(assoc % :url (str url (:crop/id %) "/")) ,,,)))

(defn get-crops-edn*
  [db url]
  (map #(select-keys % [:crop/name :crop/symbol :url])
       (all-crops db url)))

(defn get-crops-edn
  [{:keys [uri] :as request}]
  (let [db (db/current-db)]
    (get-crops-edn* db uri)))

(defn crops-layout
  [db url]
  (let [crops (all-crops db url)]
    [:div.container
     (temp/standard-get-post-h3 url)

     (temp/standard-get-layout*
       url (vocab :crops) (vocab :description)
       "text/html" [:ul#farms
                    (for [{url :url
                           name :crop/name
                           symbol :crop/symbol} crops]
                      [:li [:a {:href url} (str symbol " | " name)]])]

       "application/edn" [:code {:style "width:100%"}
                          (pr-str (get-crops-edn* db url))])]))

(defn get-crops
  [request]
  (let [db (db/current-db)]
    (common/standard-get (partial crops-layout db)
                         request)))

(defn get-crop
  [id request]
  (str "Crop no: " id " and full request: " request))

(defn get-crop-edn
  [id request]
  (let [db (db/current-db)]
    (->> (d/q '[:find ?crop-e
                :in $ ?crop-id
                :where
                [?crop-e :crop/id ?crop-id]]
              db id)
         ffirst
         (d/entity db ,,,)
         #_(map #(select-keys % [:crop/id :crop/name :crop/symbol]) ,,,)
         #_(map #(assoc % :url (str uri (:crop/id %) "/")) ,,,)))
  )















