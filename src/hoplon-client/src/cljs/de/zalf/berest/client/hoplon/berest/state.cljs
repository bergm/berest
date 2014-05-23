(ns de.zalf.berest.client.hoplon.berest.state
  (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]])
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [tailrecursion.javelin :as j :refer [cell]]
            [tailrecursion.castra  :as c :refer [mkremote]]))

(enable-console-print!)

(defc state {})
#_(cell= (println "state: " (pr-str state)))
(defc error nil)
(defc loading [])

(defc csv-result nil)
(cell= (println "csv-result: " (pr-str csv-result)))
(defc calc-error nil)
(defc calculating [])

(defc= loaded?      (not= {} state))
(defc= loading?     (seq loading))
(defc= logged-in?   (not (or (nil? state) (= {} state))))
(defc= show-login?  (and loaded? (not logged-in?)))

(def clear-error!   #(reset! error nil))

(def login! (mkremote 'berest.web.castra.api/login state error loading))
(def logout! (mkremote 'berest.web.castra.api/logout state error loading))
(def get-state (mkremote 'berest.web.castra.api/get-berest-state state error loading))
(def calculate-csv (mkremote 'berest.web.castra.api/calculate-csv csv-result calc-error calculating))
(def simulate-csv (mkremote 'berest.web.castra.api/simulate-csv csv-result calc-error calculating))

(defn init []
  (get-state)
  #_(js/setInterval #(if @logged-in? (get-state)) 100))






(comment

  (defc= user         (:user state))
  (defc= buddies      (:users state))
  (defc= convs        (sort (keys (:messages state))))
  (defc= msgs         (get-in state [:messages active-chat]))
  (defc= loop-convs   (mapv (fn [x] [x, (str/join ", " (disj x user))]) convs))


  (def switch-chat!   #(reset! active-chat @%))
  (def toggle-chat*   #(cond (contains? %1 %2) (disj %1 %2) %1 (conj %1 %2) :_ #{@user %2}))
  (def toggle-chat!   #(swap! active-chat toggle-chat* @%))


  (def send-message*  (mkremote 'berest-client.castra-api/send-message   state error loading))
  (def send-message!  #(send-message* @user @active-chat %))

  (cell=
    (let [s (get-in error [:data :state] ::nope)]
      (if-not (= ::nope s) (reset! ~(cell state) s))))


  )


