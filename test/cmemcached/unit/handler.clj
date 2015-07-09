(ns cmemcached.unit.handler
  (:require [cmemcached.handler :refer :all]
            [byte-streams :as bytes]
            [midje.sweet :refer :all])
  (:import [java.util UUID]))

(defn- uuid [] (str (UUID/randomUUID)))

(defn- handle
  [message & {:keys [cid info] :or {cid (uuid) info {}}}]
  (handle-message (.getBytes (str message "\r\n")) cid info))

(fact-group
 :unit
 (fact "version command results in version response"
       (handle "version") => #"VERSION.*"))

(fact-group
 :unit

 (fact "invalid command results in error response"
       (handle "rubbish") => "ERROR\r\n")

 (fact "empty command results in error response"
       (handle "") => "ERROR\r\n")

 (fact "set command with invalid flags results in error response"
       (handle "set key a 300 8") => "CLIENT_ERROR\r\n")

 (fact "set command with flags below zero results in error response"
       (handle "set key -1 300 8") => "CLIENT_ERROR\r\n")

 (fact "set command with flags above max unsigned int results in error response"
       (handle "set key 4294967296 300 8") => "CLIENT_ERROR\r\n")

 (fact "set command with invalid expiry results in error response"
       (handle "set key 0 a 8") => "CLIENT_ERROR\r\n")

 (fact "set command with expiry below zero results in error response"
       (handle "set key 0 -1 8") => "CLIENT_ERROR\r\n")

 (fact "set command with invalid bytes count results in error response"
       (handle "set key 0 300 a") => "CLIENT_ERROR\r\n")

 (fact "set command with bytes count below zero results in error response"
       (handle "set key 0 300 0") => "CLIENT_ERROR\r\n")

 (fact "set command with invalid cas-unique results in error response"
       (handle "cas key 0 300 8 a") => "CLIENT_ERROR\r\n")

 (fact "set command with cas-unique below zero results in error response"
       (handle "cas key 0 300 8 -1") => "CLIENT_ERROR\r\n")

 (fact "set command with cas-unique above max unsigned long results in error response"
       (handle "cas key 0 300 8 18446744073709551616") => "CLIENT_ERROR\r\n")

 (fact "set command with invalid noreply value results in error response"
       (handle "set key 0 300 8 norep_y") => "CLIENT_ERROR\r\n")

 (fact "cas command with invalid noreply value results in error response"
       (handle "cas key 0 300 8 0 n_reply") => "CLIENT_ERROR\r\n")

 (fact "incomplete command results in error response"
       (handle "set key 0 300") => "CLIENT_ERROR\r\n")

 (fact "really incomplete command results in error response"
       (handle "set key 0") => "CLIENT_ERROR\r\n")

 (fact "really, really incomplete command results in error response"
       (handle "set key") => "CLIENT_ERROR\r\n")

 (fact "really, really, really incomplete command results in error response"
       (handle "set") => "CLIENT_ERROR\r\n")

 (fact "set command where byte count and data size don't match results in error response"
       (let [cid (uuid)
             set-response (handle "set key 0 300 8" :cid cid)]
         set-response => nil
         (handle "1234567\r\n" :cid cid) => "CLIENT_ERROR\r\n")))

(fact-group
 :unit

 ;; Hmm... these aren't quite unit tests as they involve the real cache?

 (fact "valid set command and data results in stored response"
       (let [cid (uuid)
             key (uuid)
             set-response (handle (str "set " key " 0 300 8") :cid cid)
             data-response (handle "somedata\r\n" :cid cid)]
         set-response => nil
         data-response => "STORED\r\n"))

 (fact "valid set command and data results in exists response if the key is already stored"
       (let [cid (uuid)
             key (uuid)
             set-response (handle (str "set " key " 0 300 8") :cid cid)
             data-response (handle "somedata\r\n" :cid cid)
             set-response2 (handle (str "set " key " 0 300 8") :cid cid)
             data-response2 (handle "somedata\r\n" :cid cid)]
         set-response => nil
         data-response => "STORED\r\n"
         set-response2 => nil
         data-response2 => "EXISTS\r\n"))

 (fact "get command with missing key results in client error response"
       (handle "get") => "CLIENT_ERROR\r\n")

 (fact "valid get command where item does not exist results in not found response"
       (handle "get notfound") => "NOT_FOUND\r\n")

 (fact "valid get command retrieves data when it exists"
       (let [cid (uuid)
             key (uuid)
             set-response (handle (str "set " key " 0 300 8") :cid cid)
             data-response (handle "somedata\r\n" :cid cid)
             get-response (handle (str "get " key) :cid cid)]
         set-response => nil
         data-response => "STORED\r\n"
         get-response => "somedata\r\n")))
