(ns cmemcached.acceptance.spyglass
  (:require [cmemcached.acceptance.common :refer :all]
            [clojurewerkz.spyglass.client :as spy]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(def client (memoize #(spy/text-connection (str "localhost:" port))))

(fact-group
 :acceptance

 (fact "version can be retrieved from the one memcache instance"
       (let [versions (spy/get-versions (client))
             version (first (vals versions))]
         version => truthy))

 (fact "getting a value that doesn't exist results in no value response"
       (spy/get (client) "notfound") => nil)

 (fact "simple value can be set and retrieved"
       (let [key (uuid)
             value (uuid)
             ttl 300]
         (spy/set (client) key ttl value)
         (spy/get (client) key) => value)))
