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

 (fact "simple values can be set and retrieved"
       (let [key1 (uuid)
             value1 (uuid)
             key2 (uuid)
             value2 (uuid)
             key3 (uuid)
             value3 (uuid)
             ttl 300]
         (wxm (xm/set key1 value1 ttl))
         (wxm (xm/set key2 value2 ttl))
         (wxm (xm/set key3 value3 ttl))
         (wxm (xm/get key1)) => value1
         (wxm (xm/get key2)) => value2
         (wxm (xm/get key3)) => value3))

 (fact "simple values can be set and retrieved with the same client connection"
       (let [key1 (uuid)
             value1 (uuid)
             key2 (uuid)
             value2 (uuid)
             key3 (uuid)
             value3 (uuid)
             ttl 300]
         (wxm
          (xm/set key1 value1 ttl)
          (xm/set key2 value2 ttl)
          (xm/set key3 value3 ttl)
          (xm/get key1) => value1
          (xm/get key2) => value2
          (xm/get key3) => value3))))
