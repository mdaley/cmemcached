(ns cmemcached.acceptance.spyglass
  (:require [cmemcached.acceptance.common :refer :all]
            [clojurewerkz.spyglass.client :as spy]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(defn client [] (spy/text-connection (str "localhost:" port)))

(fact-group
 :acceptance

 (fact "version can be retrieved from the one memcache instance"
       (let [versions (spy/get-versions (client))
             version (first (vals versions))]
         version => #"[0-9]+\.[0-9]+\.[0-9]+\-SNAPSHOT"))

 (fact "getting a value that doesn't exist results in no value response"
       (spy/get (client) "notfound") => nil)

 (fact "simple value can be set and retrieved"
       (let [key (uuid)
             value (uuid)
             ttl 300]
         (spy/set (client) key ttl value)
         (spy/get (client) key) => value))

 (fact "simple value can be set and retrieved but then it expires and can't be retrieved"
       (let [key (uuid)
             value (uuid)
             ttl 1]
         (spy/set (client) key ttl value)
         (spy/get (client) key) => value
         (Thread/sleep 1100)
         (spy/get (client) key) => nil))

 (fact "several values can be set and all retrieved at the same time"
       (let [key1 (uuid)
             value1 (uuid)
             key2 (uuid)
             value2 (uuid)
             key3 (uuid)
             value3 (uuid)
             c (client)]
         (spy/set c key1 300 value1)
         (spy/set c key2 300 value2)
         (spy/set c key3 300 value3)
         (Thread/sleep 500)
         (let [result (spy/get-multi (client) [key1 key2 key3])]
           result => {key1 value1 key2 value2 key3 value3}))))
