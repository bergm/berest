(ns berest.init-db
  (:require [datomic.api :as d]
            [berest.datomic :as db]))

(defn delete-db
  []
  (db/delete-db! db/*db-id*))

(defn create-and-import-data
  []
  (apply db/create-db db/*db-id* db/datomic-schema-files)

  (db/register-user "michael" "#zALf!" "Michael Berg" [:admin :guest :farmer :consultant])
  (db/register-user "guest" "guest" "Guest Guest")
  (db/register-user "zalf" "fLAz" "Zalf Zalf" [:consultant])


  )





