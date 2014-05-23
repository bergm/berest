(ns de.zalf.berest.client.hoplon.rpc
  (:require-macros
    [tailrecursion.javelin :refer [defc defc=]])
  (:require
   [tailrecursion.javelin]
   [tailrecursion.castra :refer [mkremote]]))

(defc state {:random nil})
(defc error nil)
(defc loading [])

(defc= random-number (get state :random))
(defc= counter (get state :counter))

(def get-state
  (mkremote 'berest.web.castra.api/get-state state error loading))

(defn init []
  (get-state)
  (js/setInterval get-state 1000))




