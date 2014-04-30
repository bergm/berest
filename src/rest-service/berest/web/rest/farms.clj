(ns berest.web.rest.farms
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
            [clojure.edn :as edn]))


(defn vocab
  "translatable vocabulary for this page"
  [element & [lang]]
  (get-in {:farms {:lang/de "Betriebe"
                   :lang/en "farms"}
           :description {:lang/de "Hier werden alle in der Datenbank
                  gespeicherten Betriebe angezeigt."
                  :lang/en "Here will be displayed all farms
                  stored in the database."}
           :create {:lang/de "Neuen Betrieb erstellen:"
                    :lang/en "Create new farm:"}
           :create-button {:lang/de "Erstellen"
                           :lang/en "Create"}

           }
          [element (or lang common/*lang*)] "UNKNOWN element"))

(defn- db->farms
  [db full-url]
  (->> (d/q '[:find ?farm-e
              :in $
              :where
              [?farm-e :farm/id]]
            db)
       (map (rcomp first (partial d/entity db)) ,,,)
       (map #(select-keys % [:farm/id :farm/name]) ,,,)
       (map #(assoc % :url (str full-url (:farm/id %) "/")) ,,,)))

(defn get-farms-edn*
  [db full-url]
  (map #(select-keys % [:farm/id :farm/name :url]) (db->farms db full-url)))

(defn get-farms-edn
  [request]
  (let [full-url (req/request-url request)
        db (db/current-db)]
    (get-farms-edn* db full-url)))

(defn farms-layout
  [db request]
  (let [full-url (req/request-url request)
        url-path (:uri request)
        farms (db->farms db full-url)]
    [:div.container
     (temp/standard-header url-path)

     [:hr]

     (temp/standard-get-layout*
       {:url url-path
        :title (vocab :farms)
        :description (vocab :description)}
       "text/html" [:ul
                    (for [{url :url
                           farm-id :farm/id
                           name :farm/name} (sort-by :farm/name farms)]
                      [:li [:a {:href url} (str "(" farm-id ") " name)]])]

       "application/edn" [:code {:style "white-space:pre-wrap"}
                          (pr-str (get-farms-edn* db full-url))])

     [:hr]

     (temp/standard-post-layout*
       {:url url-path
        :title (vocab :create)}

       [:div.container
        (for [e (queries/get-ui-entities db :rest.ui/groups :farm)]
          (common/create-form-element db e))

        [:button.btn.btn-primary {:type :submit} (vocab :create-button)]])]))

(defn get-farms
  [request]
  (let [db (db/current-db)]
    (common/standard-get request (farms-layout db request))))















