(ns berest.datomic
  (:require clojure.set
            [crypto.password.scrypt :as pwd]
            [clojure.string :as cstr]
            [clojure.pprint :as pp]
            [clj-time.core :as ctc]
            [clj-time.coerce :as ctcoe]
            [clojure.java.io :as cjio]
            [clojure.tools.logging :as log]
            [datomic.api :as d :refer [q db]]
            [berest.util :as bu]
            [berest.helper :as bh :refer [|->]]
            [clojurewerkz.propertied.properties :as properties]))

#_(def ^:dynamic *db-id* "berest")

(def system-db-id "system")

(defn datomic-connection-string* [base-uri db-id]
  (str base-uri db-id))

(def free-local-base-uri "datomic:free://localhost:4334/")
(def free-local-connection-string (partial datomic-connection-string* free-local-base-uri))

(def free-azure-base-uri "datomic:free://humane-spaces.cloudapp.net:4334/")
(def free-azure-connection-string (partial datomic-connection-string* free-azure-base-uri))

(def infinispan-local-datomic-base-uri "datomic:inf://localhost:11222/")
(def infinispan-local-connection-string (partial datomic-connection-string* infinispan-local-datomic-base-uri))

#_(def dynamodb-base-uri "datomic:ddb://eu-west-1/berest-datomic-store/%s?aws_access_key_id=AKIAIKDIFN2XPB7ZE3SA&aws_secret_key=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")
#_(defn dynamodb-connection-string [db-id]
    (format dynamodb-base-uri db-id))

(def dynamodb-base-uri
  (do
    ;; set aws credentials
    (->> "private/db/aws-credentials.properties"
         cjio/resource
         properties/load-from
         properties/properties->map
         (map (fn [[k v]] (System/setProperty k v)) ,,,)
         dorun)
    "datomic:ddb://eu-west-1/berest-datomic-store/"))
(def dynamodb-connection-string (partial datomic-connection-string* dynamodb-base-uri))

(def datomic-connection-string dynamodb-connection-string)

(defn datomic-connection [db-id]
  (->> db-id
       datomic-connection-string
       d/connect))

(defn db-connection [db-id]
  (->> db-id
       datomic-connection-string
       d/connect))

(defn current-db [db-id]
  (some->> db-id
           datomic-connection
           d/db))

(def datomic-schema-files {:system ["private/db/berest-meta-schema.edn"
                                    "private/db/system-schema.edn"
                                    "private/db/berest-schema.edn"
                                    "private/db/rest-ui-description.edn"]
                           :berest ["private/db/berest-meta-schema.edn"
                                    "private/db/berest-schema.edn"
                                    "private/db/rest-ui-description.edn"]})

(defn apply-schemas-to-db
  [datomic-connection & schema-files]
  (->> schema-files
       (map (|-> cjio/resource slurp read-string) ,,,)
       (map (partial d/transact datomic-connection) ,,,)
       dorun))

(defn delete-db!
  [db-id]
  (->> db-id
       datomic-connection-string
       d/delete-database))

(defn create-db
  [db-id & initial-schema-files]
  (let [uri* (datomic-connection-string db-id)
        res {:db-id db-id
             :db-uri uri*}]
    (try
      (if (d/create-database uri*)
        (try
          (apply apply-schemas-to-db (d/connect uri*) initial-schema-files)
          (assoc res :success true)
          (catch Exception e
            (log/info "Couldn't apply schemas to db at uri: " uri* ", schema files where: "
                      initial-schema-files ". Removing db.")
            (delete-db! db-id)
            (assoc res :success false
                       :error-reason :exception-applying-schemas)))
        (assoc res :success false
                   :error-reason :db-alredy-existed))
      (catch Exception e
        (log/info "Couldn't create db at uri: " uri* ". Error was: " e)
        (assoc res :success false
                   :error-reason :exception-db-creation)))))

(defn bootstrap-system-db
  [& [db-id]]
  (apply create-db (or db-id system-db-id) (:berest datomic-schema-files)))





(comment
  "instarepl debugging code"

  ;system db
  (bootstrap-system-db system-db-id)
  (apply create-db system-db-id (:system datomic-schema-files))
  (d/create-database (datomic-connection-string "system"))
  (apply apply-schemas-to-db (datomic-connection system-db-id)
         (apply concat (vals datomic-schema-files)))
       (delete-db! system-db-id)

  (delete-db! "michael")
  (apply create-db "michael" (:berest datomic-schema-files))

  (d/create-database (datomic-connection-string "berest"))
  (apply apply-schemas-to-db (db-connection "michael") (:berest datomic-schema-files))

  (def ss (-> datomic-schema-files :system second))
  (def ss* ((bh/rcomp cjio/resource slurp read-string) ss))
  (d/transact (datomic-connection "system") ss*)

  (def ms (-> datomic-schema-files :berest first))
  (def ms* ((bh/rcomp cjio/resource slurp read-string) ms))
  (d/transact (datomic-connection "system") ms*)

  (def s (-> datomic-schema-files :berest second))
  (def s* ((bh/rcomp cjio/resource slurp read-string) s))
  (d/transact (datomic-connection "system") s*)

  (def rui (-> datomic-schema-files :berest (nth ,,, 2)))
  (def rui* ((bh/rcomp cjio/resource slurp read-string) rui))
  (d/transact (datomic-connection "system") rui*)

  )



(defn new-entity-ids [] (repeatedly #(d/tempid :db.part/user)))
(defn new-entity-id [] (first (new-entity-ids)))
(defn temp-entity-id [value] (d/tempid :db.part/user value))

(defn create-entities
  ([key value kvs]
    (map (fn [id [k v]] {:db/id id
                         key k
                         value v})
         (new-entity-ids) (apply array-map kvs)))
  ([ks-to-vss]
    (map #(assoc (zipmap (keys ks-to-vss) %) :db/id (new-entity-id))
         (apply map vector (vals ks-to-vss)))))

(defn create-inline-entities
  ([key value kvs]
   (mapv (fn [[k v]] {key k, value v})
        (apply array-map kvs)))
  ([ks-to-vss]
   (mapv #(zipmap (keys ks-to-vss) %)
        (apply map vector (vals ks-to-vss)))))

(defn get-entity-ids [entities] (mapv :db/id entities))
(defn get-entity-id [entity] (:db/id entity))

(defn get-entities [db entity-ids]
  (mapv (partial d/entity db) entity-ids))

(defn get-entity [db entity-id]
  (first (get-entities db [entity-id])))

(defn create-map-from-entity-ids
  [db key value entity-ids]
  (->> entity-ids
    (get-entities db ,,,)
    (map (juxt key value) ,,,)
    (into (sorted-map) ,,,)))

(defn create-map-from-entities
  [key value entities]
  (->> entities
    (map (juxt key value) ,,,)
    (into (sorted-map) ,,,)))

(defn query-for-db-id [db relation value]
  (->> (q '[:find ?db-id
            :in $ ?r ?v
            :where
            [?db-id ?r ?v]]
         db relation value)
    (map first)))

(defn unique-query-for-db-id [db relation value]
  (first (query-for-db-id db relation value)))


(defn create-dc-assertion*
  "Create a dc assertion for given year 'in-year' to define that at abs-dc-day
  the dc-state was 'dc'. Optionally a at-abs-day can be given when the
  dc state had been told the system, else abs-dc-day will be assumed."
  [in-year abs-dc-day dc & [at-abs-day]]
  {:db/id (new-entity-id)
   :assertion/at-abs-day (or at-abs-day abs-dc-day)
   :assertion/assert-dc dc
   :assertion/abs-assert-dc-day abs-dc-day})

(defn create-dc-assertion
  "Create a dc assertion for given year 'in-year' to define that at '[day month]'
  the dc-state was 'dc'. Optionally a '[at-day at-month]' can be given when the
  dc state had been told the system, else '[day month]' will be assumed"
  [in-year [day month] dc & [[at-day at-month :as at]]]
  (let [abs-dc-day (bu/date-to-doy day month in-year)
        at-abs-day (if (not-any? nil? (or at [nil]))
                       (bu/date-to-doy at-day at-month in-year)
                       abs-dc-day)]
       (create-dc-assertion* in-year abs-dc-day dc at-abs-day)))

(defn create-dc-assertions
  "create multiple assertions at one"
  [in-year assertions]
  (map #(apply create-dc-assertion in-year %) assertions))


(defn create-irrigation-donation*
	"Create datomic map for an irrigation donation given an start-abs-day and optionally
  an end-abs-day (else this will be the same as start-abs-day) and the irrigation-donation in [mm]"
	[start-abs-day donation-mm & [end-abs-day]]
  {:db/id (new-entity-id)
   :irrigation/abs-start-day start-abs-day
   :irrigation/abs-end-day (or end-abs-day start-abs-day)
   :irrigation/amount donation-mm})

(defn create-irrigation-donation
  "create datomic map for an irrigation donation"
  [in-year [start-day start-month] donation-mm & [[end-day end-month :as end-date]]]
  (let [start-abs-day (bu/date-to-doy start-day start-month in-year)
        end-abs-day (if (not-any? nil? (or end-date [nil]))
                       (bu/date-to-doy end-day end-month in-year)
                       start-abs-day)]
       (create-irrigation-donation* start-abs-day donation-mm end-abs-day)))

(defn create-irrigation-donations
  "Create multiple irrigation donation datomic maps at once"
  [in-year donations]
  (map #(apply create-irrigation-donation in-year %) donations))



;;transaction functions

(comment "moved the only transaction function to dwd.clj as it is actually pretty domain specific")


;; user management and credential functions

(defn store-credentials
  "store given credentials into db"
  [db-connection user-id password full-name roles]
  (let [enc-pwd (pwd/encrypt password)
        kw-roles (map #(->> % name (keyword "user.role" ,,,)) roles)
        creds {:db/id (d/tempid :db.part/user)
               :user/id user-id
               :user/password enc-pwd
               :user/full-name full-name
               :user/roles kw-roles}
        creds- (dissoc creds :db/id :user/password)]
    (try
      @(d/transact db-connection [creds])
      creds-
      (catch Exception e
        (log/info "Couldn't store credentials into datomic database! Data w/o pwd: [\n"
                  (dissoc creds :user/password) "\n]")
        nil))))

(comment

  (store-credentials (db-connection "system")
                     "michael" "#zALf!" "Michael Berg" [:admin :guest :farmer :consultant])

  )

(defn- register-user*
  [system-con user-id password full-name roles]
  (let [{success :success
         :as res} (apply create-db user-id (:berest datomic-schema-files))]
    (if success
      (store-credentials system-con user-id password full-name roles)
      (when-not (= :db-alredy-existed (:error-reason res))
        (delete-db! (:db-id res))))))

(defn register-user
  [user-id password full-name & {roles :roles :or {roles [:guest]}}]
  (register-user* (db-connection system-db-id)
                  user-id password full-name roles))

(comment

  (register-user "guest" "guest")

  )


(defn- credentials*
  [system-db user-id password]
  (let [ user-entity (d/q '[:find ?e
                            :in $ ?user-id
                            :where
                            [?e :user/id ?user-id]]
                          system-db user-id)
         identity (some->> user-entity
                           ffirst
                           (d/entity system-db ,,,)
                           d/touch
                           (into {} ,,,))]
    (when (and identity
               (pwd/check password (:user/password identity)))
      {:user-id (:user/id identity)
       :roles (->> (:user/roles identity)
                   (map #(-> % name keyword) ,,,)
                   (into #{} ,,,))
       :full-name (:user/full-name identity)}))
  )

(defn credentials
  ([{:keys [username password]}]
   (credentials username password))
  ([user-id password]
   (credentials (current-db system-db-id) user-id password)))


(comment
  "insta repl code"

  (credentials (current-db "berest") "michael" "#zALf!")

  (d/q '[:find ?e
         :in $
         :where
         [?e :user/id ?user-id]]
       (current-db system-db-id) "michael")

  (->> (d/q '[:find ?e
              :in $
              :where
              [?e :user/id ?user-id]]
            (current-db system-db-id) "michael")
       ffirst
       (d/entity (current-db system-db-id) ,,,)
       d/touch)

  (->> (d/q '[:find ?e
              :in $
              :where
              [?e :db/ident :user/id]]
            (current-db system-db-id))
       ffirst
       (d/entity (current-db system-db-id))
       d/touch)


  )


