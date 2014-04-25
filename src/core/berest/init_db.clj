(ns berest.init-db
  (:require [datomic.api :as d]
            [clj-time.core :as date]
            [berest.datomic :as db]
            [berest.climate.import :as climate-import]
            [berest.climate.dwd :as dwd]
            [berest.crops.import :as crop-import]
            [berest.test-data :as test-data]))

(defn delete-db
  []
  (db/delete-db! db/*db-id*))

(defn create-and-import-data
  []

  ;create initial db and schemas
  (apply db/create-db db/*db-id* db/datomic-schema-files)

  ;register some users
  (db/register-user "michael" "#zALf!" "Michael Berg" [:admin :guest :farmer :consultant])
  (db/register-user "guest" "guest" "Guest Guest")
  (db/register-user "zalf" "fLAz" "Zalf Zalf" [:consultant])

  ;add climate data
  ;local zalf climate data
  (climate-import/transact-zalf-data (db/connection))
  ;dwd data for berest field trial
  (dwd/bulk-import-dwd-data-into-datomic (date/date-time 2014 2 3)
                                         (date/date-time 2014 4 17))

  ;add crop data
  (crop-import/import-bbfastdx-crop-files-into-datomic (db/connection))

  ;add some test data
  (test-data/add-zalf-test-farm :zalf)
  (test-data/add-zalf-test-plot :zalf)


  )





