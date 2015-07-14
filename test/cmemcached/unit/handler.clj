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
       (handle "set key 0 300 -1") => "CLIENT_ERROR\r\n")

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
       (handle "set key 0 300 8\r\n1234567") => "CLIENT_ERROR\r\n"))

(fact-group
 :unit

 ;; Hmm... these aren't quite unit tests as they involve the real cache?

 (fact "valid set command and data results in stored response"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"))

 (fact "valid set command and data results in exists response if the key is already stored"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "EXISTS\r\n"))

 (fact "zero sized data can be stored and retrieved"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 0\r\n")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 0\r\n\r\nEND\r\n")))

 (fact "get command with missing key results in client error response"
       (handle "get") => "CLIENT_ERROR\r\n")

 (fact "valid get command where item does not exist results in no values followed by END"
       (handle "get notfound") => "END\r\n")

 (fact "valid get command retrieves data when it exists"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n")))

 (fact "valid get command for multiple items works when all data exists"
       (let [key1 (uuid)
             key2 (uuid)
             key3 (uuid)]
         (handle (str "set " key1 " 0 300 4\r\npear")) => "STORED\r\n"
         (handle (str "set " key2 " 0 300 6\r\nbanana")) => "STORED\r\n"
         (handle (str "set " key3 " 0 300 5\r\napple")) => "STORED\r\n"
         (handle (str "get " key1 " " key2 " " key3)) => (str "VALUE " key1 " 0 4\r\npear\r\nVALUE " key2 " 0 6\r\nbanana\r\nVALUE " key3 " 0 5\r\napple\r\nEND\r\n")))

 (fact "valid get command for multiple items works when not all data exists"
       (let [key1 (uuid)
             key2 (uuid)
             key3 (uuid)]
         (handle (str "set " key1 " 0 300 4\r\npear")) => "STORED\r\n"
         (handle (str "set " key3 " 0 300 5\r\napple")) => "STORED\r\n"
         (handle (str "get " key1 " " key2 " " key3)) => (str "VALUE " key1 " 0 4\r\npear\r\nVALUE " key3 " 0 5\r\napple\r\nEND\r\n")))

 (fact "valid get command retrieves data when it exists but not after it has timed out"
       (let [key (uuid)]
         (handle (str "set " key " 0 1 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n")
         (Thread/sleep 1100)
         (handle (str "get " key)) => "END\r\n"))

 (fact "valid get command for multiple items works when all data exists but after time out one item is gone"
       (let [key1 (uuid)
             key2 (uuid)
             key3 (uuid)]
         (handle (str "set " key1 " 0 300 4\r\npear")) => "STORED\r\n"
         (handle (str "set " key2 " 0 1 6\r\nbanana")) => "STORED\r\n"
         (handle (str "set " key3 " 0 300 5\r\napple")) => "STORED\r\n"
         (handle (str "get " key1 " " key2 " " key3)) => (str "VALUE " key1 " 0 4\r\npear\r\nVALUE " key2 " 0 6\r\nbanana\r\nVALUE " key3 " 0 5\r\napple\r\nEND\r\n")
         (Thread/sleep 1100)
         (handle (str "get " key1 " " key2 " " key3)) => (str "VALUE " key1 " 0 4\r\npear\r\nVALUE "key3 " 0 5\r\napple\r\nEND\r\n"))))

(fact-group
 :unit

 (fact "valid gets command retrieves data with cas"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "gets " key)) => (re-pattern (str "VALUE " key " 0 8 [0-9].+\r\nsomedata\r\nEND\r\n")))))
