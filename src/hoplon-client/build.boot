#!/usr/bin/env boot

#tailrecursion.boot.core/version "2.3.1"

(set-env!
 :project      'berest-hoplon-client
 :version      "0.1.0-SNAPSHOT"

 ;doesn't work right now, as the repository must be a string, even though pomegrante allows this, but boot doesn't
 #_:repositories #_#{{:url "https://my.datomic.com/repo"
                      :username "michael.berg@zalf.de"
                      :password "dfe713b3-62f0-469d-8ac9-07d6b02b0175"}}

 :dependencies '[[tailrecursion/boot.task   "2.1.1"]
                 [tailrecursion/hoplon      "5.5.1"]
                 [tailrecursion/boot.notify "2.0.0-snapshot"]
                 #_[tailrecursion/boot.ring   "0.1.0-snapshot"]
                 [org.clojure/clojurescript "0.0-2156"]

                 [cljs-ajax "0.2.3"]

                 [org.clojure/core.match "0.2.1"]

                 #_(
                 [com.datomic/datomic-pro "0.9.4556"]

                 [crypto-password "0.1.1"]

                 [buddy "0.1.0-beta4"]

                 [simple-time "0.1.1"]
                 [clj-time "0.6.0"]

                 [clojure-csv "2.0.1"]
                 [org.clojure/algo.generic "0.1.1"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [com.taoensso/timbre "2.6.3"]
                 [egamble/let-else "1.0.6"]
                 [org.clojars.pallix/analemma "1.0.0"]
                 [org.clojure/core.match "0.2.0"]
                 [com.keminglabs/c2 "0.2.3"]
                 [formative "0.3.2"]
                 [com.velisco/clj-ftp "0.3.0"]
                 [instaparse "1.2.13"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/tools.namespace "0.2.4"]
                 [clojurewerkz/propertied "1.1.0"]
                   )

                 ]
 :out-path     "../../resources/public"
 :src-paths    #{"src/hoplon"
                 "src/cljs"
                 "src/apogee"


                 ;both will be used if castra is used for rpc, for now we use the REST service (has to work anyway)
                 #_"src/castra"
                 #_"../berest/src"})

;; Static resources (css, images, etc.):
(add-sync! (get-env :out-path) #{"assets"})

(require '[tailrecursion.hoplon.boot :refer :all]
         #_'[tailrecursion.boot.task.ring   :refer [dev-server]]
         '[tailrecursion.boot.task.notify :refer [hear]]
         '[tailrecursion.castra.handler :as c])

(deftask castra
  [& specs]
  (r/ring-task (fn [_] (apply c/castra specs))))

(deftask server
  "Start castra dev server (port 8000)."
  []
  (comp (r/head) (r/dev-mode) (r/session-cookie) (r/files) (castra 'demo.api.chat) (r/jetty)))

(deftask chat-demo
         "Build the castra chat demo. Server on port 8000."
  []
  (comp (watch) (hoplon {:prerender false}) (server)))



(deftask dev
  "Build berest-client for development."
  []
  (comp (watch) (hear) (hoplon {:prerender false :pretty-print true}) #_(dev-server)))

(deftask prod
  "Build berest-client for production."
  []
  (hoplon {:optimizations :advanced}))
