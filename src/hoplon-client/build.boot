#!/usr/bin/env boot

#tailrecursion.boot.core/version "2.3.1"

(set-env!
 :project      'berest-hoplon-client
 :version      "0.1.0-SNAPSHOT"

 :repositories {"my.datomic.com" {:url "https://my.datomic.com/repo"
                                  :username "michael.berg@zalf.de"
                                  :password "dfe713b3-62f0-469d-8ac9-07d6b02b0175"}
                "jboss" "https://repository.jboss.org/nexus/content/groups/public/"
                }

 :dependencies '[[tailrecursion/boot.task   "2.1.3"]
                 [tailrecursion/hoplon      "5.8.3"]
                 [tailrecursion/boot.notify "2.0.1"]
                 [tailrecursion/boot.ring   "0.1.0"]
                 [org.clojure/clojurescript "0.0-2202"]

                 [cljs-ajax "0.2.3"]

                 [org.clojure/core.match "0.2.1"]

                 [com.datomic/datomic-pro "0.9.4766"]
                 #_[spy/spymemcached "2.8.9"]

                 [buddy "0.1.0-beta4"]
                 [crypto-password "0.1.1"]

                 #_[ring "1.2.1"]
                 #_[fogus/ring-edn "0.2.0"]

                 [hiccup "1.0.4"]

                 [simple-time "0.1.1"]
                 [clj-time "0.6.0"]
                 [com.andrewmcveigh/cljs-time "0.1.5"]

                 [clojure-csv "2.0.1"]
                 [org.clojure/algo.generic "0.1.1"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [com.taoensso/timbre "3.1.6"]
                 [org.clojars.pallix/analemma "1.0.0"]
                 [org.clojure/core.match "0.2.0"]
                 [com.keminglabs/c2 "0.2.3"]
                 [formative "0.3.2"]
                 [com.velisco/clj-ftp "0.3.0"]
                 [instaparse "1.3.2"]
                 [org.clojure/tools.logging "0.2.6"]
                 [org.clojure/tools.namespace "0.2.4"]
                 [clojurewerkz/propertied "1.1.0"]
                 ]
 :out-path     "../../resources/public"
 :src-paths    #{"src/hl"
                 "src/cljs"
                 "src/apogee"
                 "../castra-service"
                 "../../../berest-core/private-resources"

                 ;both will be used if castra is used for rpc, for now we use the REST service (has to work anyway)
                 #_"src/castra"
                 "../../../berest-core/src"})

;; Static resources (css, images, etc.):
(add-sync! (get-env :out-path) #{"assets"})

(require '[tailrecursion.hoplon.boot :refer :all]
         #_'[tailrecursion.boot.task.ring   :refer [dev-server]]
         '[tailrecursion.boot.task.notify :refer [hear]]
         '[tailrecursion.castra.task :as c]
         #_'[tailrecursion.castra.handler :as c])

(deftask development
         "Build BEREST Hoplon client for development."
         []
         (comp (watch)
               (hear)
               (hoplon {:prerender false :pretty-print true})
               (c/castra-dev-server 'de.zalf.berest.web.castra.api)))

(deftask dev-sourcemap
         []
         (comp
           (watch)
           (hear)
           (hoplon {:prerender false :pretty-print true :source-map true})
           (c/castra-dev-server 'de.zalf.berest.web.castra.api)
           #_(dev-server)))

(deftask production
  "Build BEREST hoplon client for production."
  []
  (hoplon {:optimizations :advanced}))
