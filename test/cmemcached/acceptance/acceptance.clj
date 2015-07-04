(ns cmemcached.acceptance.acceptance
  (:require [clojurewerkz.spyglass.client :as spy]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(def port (env :port 11211))

(def client (spy/text-connection (str "localhost:" port)))

(fact-group
 :acceptance

 (fact "version can be retrieved from the one memcache instance"
       (let [versions (spy/get-versions client)
             version (first (vals versions))]
         (println "VERSION:" version)
         version => truthy)))
