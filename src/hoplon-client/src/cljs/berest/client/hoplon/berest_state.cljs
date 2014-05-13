(ns berest.client.hoplon.berest-state
  (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]])
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [tailrecursion.javelin :as j :refer [cell]]
            [tailrecursion.castra  :as c :refer [mkremote]]))

(enable-console-print!)

(defc state {})
(cell= (println "state: " (pr-str state)))
(defc error         nil)
(defc loading       [])

(defc= loaded?      (not= {} state))
(defc= loading?     (seq loading))
(defc= logged-in?   (not (or (nil? state) (= {} state))))
(defc= show-login?  (and loaded? (not logged-in?)))

(def clear-error!   #(reset! error nil))

(def login! (mkremote 'berest.web.castra.api/login state error loading))
(def logout! (mkremote 'berest.web.castra.api/logout state error loading))
(def get-state (mkremote 'berest.web.castra.api/get-berest-state state error loading))
(def test (mkremote 'berest.web.castra.api/test state error loading))

(defn init []
  #_(get-state)
  (test)
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


