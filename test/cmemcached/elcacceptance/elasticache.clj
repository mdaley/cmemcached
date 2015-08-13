(ns cmemcached.elcacceptance.elasticache
  (:require [cmemcached.acceptance.common :refer :all]
            [midje.sweet :refer :all])
  (:import [java.net InetSocketAddress]
           [net.spy.memcached MemcachedClient DefaultConnectionFactory ClientMode]))

(fact-group
 :acceptance

 (fact "Elasticache client (in static mode) can connect, set value and retrieve it"
       (let [client (MemcachedClient. [(InetSocketAddress. "localhost" port)])
             key (uuid)]
         (.set client key 300 "abcd")
         (.get client key) => "abcd"))

 (fact "Elasticache client (in dynamic  mode) can connect, set value and retrieve it"
       (let [client (MemcachedClient. (DefaultConnectionFactory. ClientMode/Dynamic) [(InetSocketAddress. "localhost" port)])
             key (uuid)]
         (.set client key 300 "abcd")
         (.get client key) => "abcd")))
