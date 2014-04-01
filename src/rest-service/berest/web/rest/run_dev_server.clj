(ns berest.web.rest.run-dev-server
  (require [ring.server.standalone :as ring-server]
           [berest.web.rest.handler :as handler]))

(ring-server/serve handler/rest-service {:port 3000
                                         :open-browser? false})
