(ns berest.import.dwd-data
  (:require [clojure.java.io :as cjio]
            [clojure.string :as str]
            [clj-time.core :as ctc]
            [clj-time.format :as ctf]
            [clj-time.coerce :as ctcoe]
            [clj-time.periodic :as ctp]
            [berest.datomic :as db]
            [berest.util :as bu]
            [datomic.api :as d]
            [miner.ftp :as ftp]
            [clojure.tools.logging :as log]
            [clojure.pprint :as pp]))

(defn- parse-german-double [text]
  (double (.. java.text.NumberFormat (getInstance java.util.Locale/GERMAN) (parse text))))

(defn parse-prognosis-data
  "parse a DWD prognosis data file and return datomic transaction data"
  [data]
  (let [data* (str/split-lines data)
        data** (->> data* (drop 7 ,,,) (take 9))
        stations (-> data** first (str/split ,,, #"\s+"))]
    (for [line (drop 3 data**)
          :let [line* (str/split line #"\s+")
                date (first line*)
                date* (->> date (ctf/parse (ctf/formatter "ddMMyyyy") ,,,) ctcoe/to-date)]
          [station [rr-s vp-t gs tm]] (map vector
                                           (rest stations)
                                           (partition 4 (rest line*)))]
      {:weather-station/id (str "dwd_" station)
       :weather-station/data {:weather-data/prognosis-data? true
                              :weather-data/date date*
                              :weather-data/precipitation (parse-german-double rr-s)
                              :weather-data/evaporation (parse-german-double vp-t)
                              :weather-data/average-temperature (parse-german-double tm)
                              :weather-data/global-radiation (parse-german-double gs)}})))

(comment "instarepl debugging code"

  (def pdata
    #_(slurp "resources/private/climate/FY60DWLA-20130530_0815.txt")
    (slurp "resources/private/climate/FY60DWLA-20140203_0915.txt"))
  (def pdata* (parse-and-transform-prognosis-data pdata))
  (pp/pprint pdata*)

  )

(defn parse-measured-data
  "parse ad DWD measured data file and return ready datomic transaction data"
  [data]
  (let [data* (str/split-lines data)
        data** (->> data* (drop 6 ,,,) (take 3))
        stations (-> data** first (str/split ,,, #"\s+"))]
    (for [line (drop 2 data**)
          :let [line* (str/split line #"\s+")
                date (first line*)
                date* (->> date (ctf/parse (ctf/formatter "dd.MM.yyyy") ,,,) ctcoe/to-date)]
          [station [rr-s vp-t gs tm]] (map vector
                                           (rest stations)
                                           (partition 4 (rest line*)))]
      {:weather-station/id (str "dwd_" station)
       :weather-station/data {:weather-data/prognosis-data? false
                              :weather-data/date date*
                              :weather-data/precipitation (parse-german-double rr-s)
                              :weather-data/evaporation (parse-german-double vp-t)
                              :weather-data/average-temperature (parse-german-double tm)
                              :weather-data/global-radiation (parse-german-double gs)}})))

(comment "instarepl debugging code"

  (def mdata
    #_(slurp "resources/private/climate/FY60DWLB-20130526_0815.txt")
    (slurp "resources/private/climate/FY60DWLB-20140203_0915.txt"))
  (def mdata* (parse-and-transform-measured-data mdata))
  (pp/pprint mdata*)
  (as-transaction-fns mdata*)

  )

(defn add-data
  "A transaction function creating data and just allowing unique data per station and day"
  [db data]
  (let [station-id (:weather-station/id data)
        {date :weather-data/date
         prognosis? :weather-data/prognosis-data?} (:weather-station/data data)
        q (datomic.api/q '[:find ?se ?e
                           :in $ ?station-id ?date ?prognosis?
                           :where
                           [?se :weather-station/id ?station-id]
                           [?se :weather-station/data ?e]
                           [?e :weather-data/date ?date]
                           [?e :weather-data/prognosis-data? ?prognosis?]]
                         db station-id date prognosis?)
        [station-entity-id data-entity-id] (first q)
        data* (if data-entity-id
                (assoc-in data [:weather-station/data :db/id] data-entity-id)
                data)]
    ;always create a temporary db/id, will be upsert if station exists already
    [(assoc data* :db/id (datomic.api/tempid :berest.part/climate))]))


(comment "insert transaction function into db, without full schema reload"

  @(d/transact (db/connection)
            [(read-string "{:db/id #db/id[:berest.part/climate]
  :db/ident :weather-station/add-data
  :db/doc \"A transaction function creating data and just allowing unique data per station and day\"
  :db/fn #db/fn {:lang \"clojure\"
                 :params [db data]
                 :code \"(let [station-id (:weather-station/id data)
       {date :weather-data/date
         prognosis? :weather-data/prognosis-data?} (:weather-station/data data)
        q (datomic.api/q '[:find ?se ?e
                           :in $ ?station-id ?date ?prognosis?
                           :where
                           [?se :weather-station/id ?station-id]
                           [?se :weather-station/data ?e]
                           [?e :weather-data/date ?date]
                           [?e :weather-data/prognosis-data? ?prognosis?]]
                         db station-id date prognosis?)
        [station-entity-id data-entity-id] (first q)
        data* (if data-entity-id
                (assoc-in data [:weather-station/data :db/id] data-entity-id)
                data)]
    ;always create a temporary db/id, will be upsert if station exists already
    [(assoc data* :db/id (datomic.api/tempid :berest.part/climate))])\"}}")])

  )


(comment "instarepl test"

  (add-data (db/current-db) {:weather-station/id "dwd_N652",
                             :weather-station/data
                             {:weather-data/prognosis-data? true,
                              :weather-data/date #inst "2014-02-08T00:00:00.000-00:00",
                              :weather-data/precipitation 4.5,
                              :weather-data/evaporation 0.7,
                              :weather-data/average-temperature 4.2,
                              :weather-data/global-radiation 444.0}})

  (datomic.api/q '[:find ?se ?station-id
                 :in $
                 :where
                 [?se :weather-station/id ?station-id]
                 #_[?se :weather-station/data ?e]
                 #_[?e :weather-data/date ?date]]
               (db/current-db) "dwd_10162" #inst "2014-02-04T00:00:00.000-00:00")

  )

(def kind-pattern #"FY60DWL(A|B)-\d{8}_\d{4}.txt")

(def date-pattern #"FY60DWL\w-(\d{8})_\d{4}.txt")

#_(defn make-filename [kind date & {:keys [h min] :or {h 9, min 15}}]
  (str "FY60DWL" ({:prognosis "A"
                   :measured "B"} kind)
       "-" (ctf/unparse (ctf/formatter "yyyyMMdd") date) "_" (format "%02d%02d" h min) ".txt"))

(comment "instarepl debug code"

  (make-prognosis-filename (ctc/date-time 2013 6 3))

  ;real ftp seams to be not necessary for just getting data (at least for anonymous access and co)
  (def t (ftp/with-ftp [client "ftp://anonymous:pwd@tran.zalf.de/pub/net/wetter"]
                       (ftp/client-get-stream client (make-prognosis-filename (ctc/date-time 2013 6 3)))))

  (clojure.java.io/reader t)

  )

(comment

  (def files (ftp/list-files "ftp://anonymous@tran.zalf.de/pub/net/wetter/"))

  )

(defn import-dwd-data-into-datomic*
  "import the dwd data under the given url [and filename] into datomic"
  ([ftp-url filename]
   (try
     (let [ftp-url-to-filename (str ftp-url filename)
           kind-identifier (second (re-find kind-pattern filename))
           data (try
                  (slurp ftp-url-to-filename)
                  (catch Exception e
                    (log/info (str "Couldn't read file from ftp server! URL was " ftp-url-to-filename))
                    (throw e)))
           transaction-data (case kind-identifier
                              "A" (parse-prognosis-data data)
                              "B" (parse-measured-data data))

           ;insert transaction data via :weather-station/add-data transaction function, to create unique data per station and day
           transaction-data->add-data (map #(vector :weather-station/add-data %) transaction-data)]
       (try
         @(d/transact (db/connection) transaction-data->add-data)
         (catch Exception e
           (log/info "Couldn't write dwd data to datomic! data: [\n" transaction-data->add-data "\n]")
           (throw e)))
       true)
     (catch Exception _ false))))

(defn import-dwd-data-into-datomic
  "import the dwd data at the given dates into datomic"
  [& dates]
  (let [dates* (or dates [(ctc/now)])
        dates** (map #(ctf/unparse (ctf/formatter "yyyyMMdd") %) dates*)
        url "ftp://anonymous@tran.zalf.de/pub/net/wetter/"
        all-files (ftp/list-files url)
        grouped-files (group-by #(second (re-find date-pattern %)) all-files)
        grouped-files-at-dates (select-keys grouped-files dates**)]
    (doseq [[d files-at-date] grouped-files-at-dates
            file-at-date files-at-date]
      (import-dwd-data-into-datomic* (str/replace-first url "anonymous@" "") file-at-date))))

(defn bulk-import-dwd-data-into-datomic
  [from-date to-date]
  (apply import-dwd-data-into-datomic
         (take (ctc/in-days (ctc/interval from-date (ctc/plus to-date (ctc/days 1))))
               (ctp/periodic-seq from-date (ctc/days 1)))))

(comment

  (import-dwd-data-into-datomic (ctc/date-time 2014 2 3))

  (bulk-import-dwd-data-into-datomic (ctc/date-time 2014 2 3)
                                     (ctc/date-time 2014 4 25))

  )

