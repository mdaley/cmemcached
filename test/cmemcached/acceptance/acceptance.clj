(ns cmemcached.acceptance.acceptance
  (:require [cmemcached.acceptance.common :refer :all]
            [clojurewerkz.spyglass.client :as spy]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(fact-group
 :acceptance

 (fact "version can be retrieved from the one memcache instance"
       (let [versions (spy/get-versions client)
             version (first (vals versions))]
         (println "VERSION:" version)
         version => truthy))

 (fact "simple value can be set and retrieved"
       (let [key (uuid)
             value (uuid)
             ttl 300
             response (spy/set client key ttl value)]
         (println "RESPONSE:" @response))))
