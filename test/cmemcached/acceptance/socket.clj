(ns cmemcached.acceptance.socket
  (:require [cmemcached.acceptance.common :refer :all]
            [aleph.tcp :as tcp]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [midje.sweet :refer :all]))

(defn create-client
  []
  (tcp/client {:host "localhost" :port port}))

(fact-group
 :acceptance

 (fact "Obtain version over socket connection"
       (let [client @(create-client)]
         @(s/put! client "version\r\n") => true
         (String. @(s/take! client)) => #"[0-9]+\.[0-9]+\.[0-9]+\-SNAPSHOT\r\n"))

 (fact "Value can be set and retrieved"
       (let [client @(create-client)
             key (uuid)]
         @(s/put! client (str "set " key " 0 300 4\r\n")) => true
         @(s/put! client "abcd\r\n") => true
         (String. @(s/take! client)) => "STORED\r\n"
         @(s/put! client (str "get " key "\r\n")) => true
         (String. @(s/take! client)) => (str "VALUE " key " 0 4\r\nabcd\r\nEND\r\n")))

 (fact "Value can be set and retrieved (separated messages for command and data)"
       (let [client @(create-client)
             key (uuid)]
         @(s/put! client (str "set " key " 0 300 4\r\n")) => true
         (Thread/sleep 1000)
         @(s/put! client "abcd\r\n") => true
         (String. @(s/take! client)) => "STORED\r\n"
         @(s/put! client (str "get " key "\r\n")) => true
         (String. @(s/take! client)) => (str "VALUE " key " 0 4\r\nabcd\r\nEND\r\n")))

 (fact "Several values can be set and retrieved"
       (let [client @(create-client)
             key1 (uuid)
             value1 (uuid)
             key2 (uuid)
             value2 (uuid)
             key3 (uuid)
             value3 (uuid)]
         @(s/put! client (str "set " key1 " 0 300 36\r\n")) => true
         @(s/put! client (str value1 "\r\n")) => true
         (String. @(s/take! client)) => "STORED\r\n"
         @(s/put! client (str "set " key2 " 0 300 36\r\n")) => true
         @(s/put! client (str value2 "\r\n")) => true
         (String. @(s/take! client)) => "STORED\r\n"
         @(s/put! client (str "set " key3 " 0 300 36\r\n")) => true
         @(s/put! client (str value3 "\r\n")) => true
         (String. @(s/take! client)) => "STORED\r\n"
         @(s/put! client (str "get " key1 "\r\n")) => true
         (String. @(s/take! client)) => (str "VALUE " key1 " 0 36\r\n" value1 "\r\nEND\r\n")
         @(s/put! client (str "get " key2 "\r\n")) => true
         (String. @(s/take! client)) => (str "VALUE " key2 " 0 36\r\n" value2 "\r\nEND\r\n")
         @(s/put! client (str "get " key3 "\r\n")) => true
         (String. @(s/take! client)) => (str "VALUE " key3 " 0 36\r\n" value3 "\r\nEND\r\n"))))
