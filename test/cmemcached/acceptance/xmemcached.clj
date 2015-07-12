(ns cmemcached.acceptance.xmemcached
  (:require [cmemcached.acceptance.common :refer :all]
            [clj-xmemcached.core :as xm]
            [midje.sweet :refer :all]))

(def client (xm/memcached (str "localhost:" port) :heartbeat false))

(defmacro wxm
        [& body]
        `(xm/with-client client ~@body))

(fact-group
 :acceptance

 ;; TODO: Don't think direct version call is implemented?
 ;; (fact "version can be retrieved from the one memcache instance"
 ;;       (let [versions (xm/set)
 ;;             version (first (vals versions))]
 ;;         (println "VERSION:" version)
 ;;         version => truthy))

 (fact "simple value can be set and retrieved"
       (let [key (uuid)
             value (uuid)
             ttl 300
             response (wxm
                        (xm/set key value ttl))
             ;;response (spy/get (client) key)
             ]
         (println "RESPONSE:" response))))
