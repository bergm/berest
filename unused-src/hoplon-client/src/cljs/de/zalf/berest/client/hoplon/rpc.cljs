(ns de.zalf.berest.client.hoplon.rpc
  (:require-macros
    [tailrecursion.javelin :refer [defc defc=]])
  (:require
   [tailrecursion.javelin]
   [tailrecursion.castra :refer [mkremote]]
   [de.zalf.berest.client.hoplon.state :as s]))

(def login! (mkremote 'de.zalf.berest.web.castra.api/login s/state s/error s/loading))
(def logout! (mkremote 'de.zalf.berest.web.castra.api/logout s/state s/error s/loading))
(def get-state (mkremote 'de.zalf.berest.web.castra.api/get-berest-state s/state s/error s/loading))
(def get-full-selected-crops (mkremote 'de.zalf.berest.web.castra.api/get-state-with-full-selected-crops s/state s/error s/loading))
(def calculate-csv (mkremote 'de.zalf.berest.web.castra.api/calculate-csv s/csv-result s/calc-error s/calculating))
(def simulate-csv (mkremote 'de.zalf.berest.web.castra.api/simulate-csv s/csv-result s/calc-error s/calculating))




