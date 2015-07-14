(ns cmemcached.acceptance.spyglass
  (:require [cmemcached.acceptance.common :refer :all]
            [clojurewerkz.spyglass.client :as spy]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(def client (memoize #(spy/text-connection (str "localhost:" port))))

(fact-group
 :acceptance

 (fact "version can be retrieved"
       (let [versions (spy/get-versions (client))
             version (first (vals versions))]
         (println "VERSION:" version)
         version => truthy))

 (fact "simple value can be set and retrieved"
       (let [key (uuid)
             value (uuid)
             ttl 300
             response (.set (client) key ttl value)
             response (.get (client) key)
             ]
         (println "RESPONSE:" response))))
