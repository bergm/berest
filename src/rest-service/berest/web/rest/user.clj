(ns berest.web.rest.user
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
  (get-in {:users {:lang/de "Nutzer"
                   :lang/en "users"}
           :description {:lang/de "Hier werden alle in der Datenbank
                  gespeicherten Nutzer angezeigt."
                         :lang/en "Here will be displayed all users
                  stored in the database."}
           :create {:lang/de "Neuen Nutzer erstellen:"
                    :lang/en "Create new user:"}
           :create-button {:lang/de "Erstellen"
                           :lang/en "Create"}

           }
          [element (or lang common/*lang*)] "UNKNOWN element"))


(defn create-user-layout [db]
  [:div.container
   (for [e (queries/get-ui-entities db :rest.ui/groups :user)]
     (common/create-form-element db e))

   [:button.btn.btn-primary {:type :submit} (vocab :create-button)]])

;;all users

(defn- all-users
  [db full-url]
  (->> (d/q '[:find ?user-e
              :in $
              :where
              [?user-e :user/id]]
            db)
       (map (rcomp first (partial d/entity db)) ,,,)
       (map #(select-keys % [:user/id :user/full-name]) ,,,)
       (map #(assoc % :url (str full-url (:user/id %) "/")) ,,,)))

(defn get-users-edn*
  [db full-url]
  (map #(select-keys % [:user/id :user/full-name :url]) (all-users db full-url)))

(defn get-users-edn
  [request]
  (let [full-url (req/request-url request)
        db (db/current-db)]
    (get-users-edn* db full-url)))

(defn users-layout
  [db request]
  (let [full-url (req/request-url request)
        url-path (:uri request)
        users (all-users db full-url)]
    [:div.container
     (temp/standard-get-post-header url-path)

     (temp/standard-get-layout*
       {:url url-path
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

        [:button.btn.btn-primary {:type :submit} (vocab :create-button)]])]))

(defn get-users
  [request]
  (let [db (db/current-db)]
    (common/standard-get request
                         (users-layout db request))))


;;single user


(defn users-layout*
[db request]
(let [db (db/current-db)
      full-url (req/request-url request)
      url-path (:uri request)
      users (all-users db full-url)]
  (hp/html5
    (common/head (str "GET " url-path))
    (common/body
      nil #_(auth/get-identity request)

      [:div.container
       (temp/standard-header url-path :get)

       [:div.container
        (for [e (queries/get-ui-entities db :rest.ui/groups :user)]
          (common/create-form-element db e))

        #_[:button.btn.btn-primary {:type :submit} (vocab :create-button)]]
       ]))))


(defn user-layout [db url]
  [:div.container
   (temp/standard-header url)

   (temp/standard-get-layout {:url url
                              :get-title (vocab :users)
                              :description (vocab :show)
                              :get-id-fn :user/id
                              :get-name-fn :user/name
                              :entities (queries/get-entities db :user/id)
                              :sub-entity-path "user/"})

   (temp/standard-post-layout {:url url
                               :post-title (vocab :create)
                               :post-layout-fn (partial create-user-layout db)})])

(defn get-user
  [request]
  (let [db (db/current-db)]
    (str "user data for user-id: request: " request)

    #_(common/standard-get (partial user-layout db)
                         request)))

(defn create-user
  [request]

  )

(defn update-user
  [request]

  )

