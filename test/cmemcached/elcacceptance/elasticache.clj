(ns cmemcached.elcacceptance.elasticache
  (:require [cmemcached.acceptance.common :refer :all]
            [midje.sweet :refer :all])
  (:import [java.net InetSocketAddress]
           [net.spy.memcached MemcachedClient DefaultConnectionFactory ClientMode]))

(defn- static-client
  []
  (MemcachedClient. [(InetSocketAddress. "localhost" port)]))

(defn- dynamic-client
  []
  (MemcachedClient. (DefaultConnectionFactory. ClientMode/Dynamic) [(InetSocketAddress. "localhost" port)]))

(fact-group
 :acceptance

 (fact "Elasticache client (in static mode) can connect, set value and retrieve it"
       (let [client (static-client)
             key (uuid)]
         (.set client key 300 "abcd")
         (.get client key) => "abcd"))

 (fact "Elasticache client (in dynamic  mode) can connect, set value and retrieve it"
       (let [client (dynamic-client)
             key (uuid)]
         (.set client key 300 "abcd")
         (.get client key) => "abcd"))

 (fact "Elasticache client (in dynamic  mode) can connect, set several values and retrieve them"
       (let [client (dynamic-client)
             key1 (uuid)
             key2 (uuid)
             key3 (uuid)]
         (.set client key1 300 "abcd")
         (.set client key2 300 "defg")
         (.set client key3 300 "qwer")
         (.get client key1) => "abcd"
         (.get client key2) => "defg"
         (.get client key3) => "qwer"))

 (fact "Elasticache client (in dynamic  mode) can connect, set value and retrieve it but not after ttl has passed"
       (let [client (dynamic-client)
             key (uuid)]
         (.set client key 1 "abcd")
         (.get client key) => "abcd"
         (Thread/sleep 1100)
         (.get client key) => nil)))
