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
            [ring.util.request :as req]
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

(defn- all-crops
  [db full-url]
  (->> (d/q '[:find ?crop-e
              :in $
              :where
              [?crop-e :crop/id]]
            db)
       (map (rcomp first (partial d/entity db)) ,,,)
       (map #(select-keys % [:crop/id :crop/name :crop/symbol]) ,,,)
       (map #(assoc % :url (str full-url (:crop/id %) "/")) ,,,)))

(defn get-crops-edn*
  [db full-url]
  (map #(select-keys % [:crop/id :crop/name :crop/symbol :url])
       (all-crops db full-url)))

(defn get-crops-edn
  [request]
  (let [full-url (req/request-url request)
        db (db/current-db)]
    (get-crops-edn* db full-url)))

(defn crops-layout
  [db request]
  (let [full-url (req/request-url request)
        url-path (:uri request)
        crops (all-crops db full-url)]
    [:div.container
     (temp/standard-header url-path)

     (temp/standard-get-layout*
       {:url-path url-path
        :title (vocab :crops)
        :description (vocab :description)}
       "text/html" [:ul
                    (for [{url :url
                           crop-id :crop/id
                           name :crop/name
                           symbol :crop/symbol} (sort-by :crop/name crops)]
                      [:li [:a {:href url} (str "(" crop-id ") " symbol " | " name)]])]

       "application/edn" [:code {:style "white-space:pre-wrap"}
                          (pr-str (get-crops-edn* db full-url))])]))

(defn get-crops
  [request]
  (let [db (db/current-db)]
    (common/standard-get request
                         (crops-layout db request))))

(defn get-crop
  [id request]
  (let [db (db/current-db)
        full-url (req/request-url request)
        url-path (:uri request)
        users (all-users db full-url)]
    [:div.container
     (temp/standard-header url-path)

     (temp/standard-get-layout*
       {:url-path url-path
        :title (vocab :users)
        :description (vocab :description)}
       "text/html" [:ul
                    (for [{url :url
                           user-id :user/id
                           name :user/full-name} (sort-by :user/id users)]
                      [:li [:a {:href url} (str "(" user-id ") " name)]])]

       "application/edn" [:code {:style "white-space:pre-wrap"}
                          (pr-str (get-users-edn* db full-url))])

     [:hr]
     (temp/standard-post-layout*
       {:url url-path
        :title (vocab :create)}

       [:div.container
        (for [e (queries/get-ui-entities db :rest.ui/groups :user)]
          (common/create-form-element db e))

        [:button.btn.btn-primary {:type :submit} (vocab :create-button)]])])


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















