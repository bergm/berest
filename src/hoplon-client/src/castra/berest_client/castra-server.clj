(ns berest-client.castra-server
  (:require
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.cookie :refer [cookie-store]]
   [ring.middleware.file :refer [wrap-file]]
   [ring.middleware.file-info :refer [wrap-file-info]]
   [tailrecursion.castra.handler :refer [castra]]))

(def server (atom nil))

(defn app [port public-path]
  (->
   (castra 'berest-client.castra-api)
   (wrap-session {:store (cookie-store {:key "a 16-byte secret"})})
   (wrap-file public-path)
   (wrap-file-info)
   (run-jetty {:join? false :port port})))

(defn start-server
  "Start berest-client castra server (port 33333)."
  [port public-path]
  (swap! server #(or % (app port public-path))))

(defn run-task
  [port public-path]
  (.mkdirs (java.io.File. public-path))
  (start-server port public-path)
  (fn [continue]
    (fn [event]
      (continue event))))

(defn -main
  "I don't do a whole lot."
  [& args])
