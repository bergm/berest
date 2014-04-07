(ns berest.client.hoplon.state
  (:require-macros
    [tailrecursion.javelin :refer [defc defc= cell=]])
  (:require
    [clojure.set           :as cs]
    [clojure.string        :as s]
    [tailrecursion.javelin :as j :refer [cell]]
    [tailrecursion.castra  :as c :refer [mkremote]]))

(set! cljs.core/*print-fn* #(.log js/console %))

(defc state         {})
(defc error         nil)
(defc loading       [])
(defc active-chat   nil)

(defc= loaded?      (not= {} state))
(defc= loading?     (seq loading))
(defc= logged-in?   (not (or (nil? state) (= {} state))))
(defc= show-chat?   (and loaded? logged-in?))
(defc= show-login?  (and loaded? (not logged-in?)))

(defc= user         (:user state))
(defc= buddies      (:users state))
(defc= convs        (sort (keys (:messages state))))
(defc= msgs         (get-in state [:messages active-chat]))
(defc= loop-convs   (mapv (fn [x] [x, (s/join ", " (disj x user))]) convs))

(def clear-error!   #(reset! error nil))
(def switch-chat!   #(reset! active-chat @%))
(def toggle-chat*   #(cond (contains? %1 %2) (disj %1 %2) %1 (conj %1 %2) :_ #{@user %2}))
(def toggle-chat!   #(swap! active-chat toggle-chat* @%))

(def get-state      (mkremote 'berest-client.castra-api/get-state      state error (cell nil)))
(def register!      (mkremote 'berest-client.castra-api/register       state error loading))
(def login!         (mkremote 'berest-client.castra-api/login          state error loading))
(def logout!        (mkremote 'berest-client.castra-api/logout         state error loading))
(def send-message*  (mkremote 'berest-client.castra-api/send-message   state error loading))
(def send-message!  #(send-message* @user @active-chat %))

(cell=
  (let [s (get-in error [:data :state] ::nope)]
    (if-not (= ::nope s) (reset! ~(cell state) s))))

(defn init []
  (get-state)
  (js/setInterval #(if @logged-in? (get-state)) 200))
