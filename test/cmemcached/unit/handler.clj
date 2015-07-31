(ns cmemcached.unit.handler
  (:require [clojure.string :refer [split]]
            [cmemcached.handler :refer :all]
            [byte-streams :as bytes]
            [midje.sweet :refer :all])
  (:import [java.util UUID]))

(defn- uuid [] (str (UUID/randomUUID)))

(defn- handle
  [message & {:keys [cid info] :or {cid (uuid) info {}}}]
  (handle-incoming (.getBytes (str message "\r\n")) cid info))

(fact-group
 :unit :version
 (fact "version command results in version response"
       (handle "version") => #"VERSION.*"))

(fact-group
 :unit :server
 (fact "handler exception results in server error response"
       (handle "set key 0 300 8\r\nsomedata") => "SERVER_ERROR error message\r\n"
       (provided
        (handle-command anything anything anything anything) =throws=> (Exception. "error message"))))

(fact-group
 :unit :invalid-commands

 (fact "invalid command results in error response"
       (handle "rubbish") => "ERROR\r\n")

 (fact "empty command results in error response"
       (handle "") => "ERROR\r\n")

 (fact "set command with invalid flags results in error response"
       (handle "set key a 300 8\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "set command with flags below zero results in error response"
       (handle "set key -1 300 8\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "set command with flags above max unsigned int results in error response"
       (handle "set key 4294967296 300 8\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "set command with invalid expiry results in error response"
       (handle "set key 0 a 8\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "set command with expiry below zero results in error response"
       (handle "set key 0 -1 8\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "set command with invalid bytes count results in error response"
       (handle "set key 0 300 a\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "set command with bytes count below zero results in error response"
       (handle "set key 0 300 -1\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "check and set command with invalid cas-unique results in error response"
       (handle "cas key 0 300 8 a\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "set command with cas-unique below zero results in error response"
       (handle "cas key 0 300 8 -1\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "set command with cas-unique above max unsigned long results in error response"
       (handle "cas key 0 300 8 18446744073709551616\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "set command with invalid noreply value results in error response"
       (handle "set key 0 300 8 norep_y\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "cas command with invalid noreply value results in error response"
       (handle "cas key 0 300 8 0 n_reply\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "incomplete command results in error response"
       (handle "set key 0 300\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "really incomplete command results in error response"
       (handle "set key 0\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "really, really incomplete command results in error response"
       (handle "set key\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "really, really, really incomplete command results in error response"
       (handle "set\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "set command where byte count and data size don't match results in error response"
       (handle "set key 0 300 8\r\n1234567") => "CLIENT_ERROR\r\n")

 (fact "set command where byte count and data size don't match results in error response even when noreply is present"
       (handle "set key 0 300 8 noreply\r\n1234567") => "CLIENT_ERROR\r\n")

 (fact "set command with cas-unique above max unsigned long results in error response even when noreply is present"
       (handle "cas key 0 300 8 18446744073709551616 noreply\r\nsomedata") => "CLIENT_ERROR\r\n")

 (fact "invalid add command with noreply results in error reponse even when noreply is present"
       (handle (str "add key 0 300 -1 noreply\r\nsomedata")) => "CLIENT_ERROR\r\n")

 (fact "invalid replace command with noreply results in error reponse even when noreply is present"
       (handle (str "replace key 0 a 8 noreply\r\nsomedata")) => "CLIENT_ERROR\r\n")

 (fact "invalid append command with noreply results in error reponse even when noreply is present"
       (handle (str "append key 0 a 8 noreply\r\nsomedata")) => "CLIENT_ERROR\r\n")

 (fact "invalid prepend command with noreply results in error reponse even when noreply is present"
       (handle (str "prepend key q 300 8 noreply\r\nsomedata")) => "CLIENT_ERROR\r\n")

 (fact "invalid check and set command with noreply results in error reponse even when noreply is present"
       (handle (str "cas key q 300 8 12345678 noreply\r\nsomedata")) => "CLIENT_ERROR\r\n"))

(fact-group
 :unit :get-and-set

 ;; Hmm... these aren't quite unit tests as they involve the real cache?

 (fact "valid set command and data results in stored response"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"))

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

 (fact "valid set command with same key overrides previous set"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "set " key " 10 300 6\r\nzzzzzz")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 10 6\r\nzzzzzz\r\nEND\r\n")))

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
 :unit :add-and-replace

 (fact "valid add command adds data that can then be retrieved"
       (let [key (uuid)]
         (handle (str "add " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n")))

 (fact "valid add command fails if the key already exists"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "add " key " 10 300 6\r\nzzzzzz")) => "NOT_STORED\r\n"))

 (fact "valid replace command fails if the key does not exist"
       (let [key (uuid)]
         (handle (str "replace " key " 0 300 8\r\nsomedata")) => "NOT_STORED\r\n"))

 (fact "valid replace command succeeds if the key exists"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "replace " key " 10 300 6\r\nzzzzzz")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 10 6\r\nzzzzzz\r\nEND\r\n"))))

(fact-group
 :unit :append-and-prepend

 (fact "append updates data correctly"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "append " key " 10 300 6\r\nzzzzzz")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 14\r\nsomedatazzzzzz\r\nEND\r\n")))

 (fact "prepend updates data correctly"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "prepend " key " 10 300 6\r\nzzzzzz")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 14\r\nzzzzzzsomedata\r\nEND\r\n"))))

(fact-group
 :unit :delete

 (fact "deleting an item that does not exist results in not found"
       (handle "delete doesnotexist") => "NOT_FOUND\r\n")

  (fact "deleting an item that does exist works; and then it can't be found any more"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n")
         (handle (str "delete " key)) => "DELETED\r\n"
         (handle (str "get " key)) => "END\r\n")))

(fact-group
 :unit :cas

 (fact "valid gets command retrieves data with cas"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "gets " key)) => (re-pattern (str "VALUE " key " 0 8 [0-9].+\r\nsomedata\r\nEND\r\n"))))

 (fact "check and set with correct cas value succeeds (expiry and flags are updated)"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (let [v (handle (str "gets " key))
               cas (nth (split v #"\s+") 4)]
           (handle (str "cas " key " 10 1 6 " cas "\r\nzzzzzz")) => "STORED\r\n"
           (handle (str "get " key)) => (str "VALUE " key " 10 6\r\nzzzzzz\r\nEND\r\n")
           (Thread/sleep 1100)
           (handle (str "get " key)) => "END\r\n")))

 (fact "check and set with incorrect cas value results in exists response"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (let [v (handle (str "gets " key))
               cas (bigint (nth (split v #"\s+") 4))]
           (handle (str "cas " key " 10 1 6 " (inc cas) "\r\nzzzzzz")) => "EXISTS\r\n"
           (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n"))))

 (fact "check and set with non-existent key results in not found response"
       (let [key (uuid)]
           (handle (str "cas " key " 10 1 6 12345678\r\nzzzzzz")) => "NOT_FOUND\r\n")))

(fact-group
 :unit :noreply

 (fact "set with noreply really does store data even though there is no response"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8 noreply\r\nsomedata")) => ""
         (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n")))

 (fact "add with noreply adds data that can then be retrieved even though there is no response"
       (let [key (uuid)]
         (handle (str "add " key " 0 300 8 noreply\r\nsomedata")) => ""
         (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n")))

 (fact "valid replace command with noreply succeeds even though there is no response"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "replace " key " 10 300 6 noreply\r\nzzzzzz")) => ""
         (handle (str "get " key)) => (str "VALUE " key " 10 6\r\nzzzzzz\r\nEND\r\n")))

 (fact "valid add command with noreply for an non-existent key gives no response"
       (let [key (uuid)]
         (handle (str "add " key " 10 300 6 noreply\r\nzzzzzz")) => ""))

 (fact "valid replace command with noreply for an non-existent key gives no response"
       (let [key (uuid)]
         (handle (str "replace " key " 10 300 6 noreply\r\nzzzzzz")) => ""))

 (fact "append with noreply updates data correctly even though there is no response"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "append " key " 10 300 6 noreply\r\nzzzzzz")) => ""
         (handle (str "get " key)) => (str "VALUE " key " 0 14\r\nsomedatazzzzzz\r\nEND\r\n")))

 (fact "prepend with noreply updates data correctly even though there is no response"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "prepend " key " 10 300 6 noreply\r\nzzzzzz")) => ""
         (handle (str "get " key)) => (str "VALUE " key " 0 14\r\nzzzzzzsomedata\r\nEND\r\n")))

 (fact "valid append command with noreply for an non-existent key gives no response"
       (let [key (uuid)]
         (handle (str "append " key " 10 300 6 noreply\r\nzzzzzz")) => ""))

 (fact "valid prepend command with noreply for an non-existent key gives no response"
       (let [key (uuid)]
         (handle (str "prepend " key " 10 300 6 noreply\r\nzzzzzz")) => ""))

 (fact "check and set with noreply and with correct cas value succeeds even though there is no response"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (let [v (handle (str "gets " key))
               cas (nth (split v #"\s+") 4)]
           (handle (str "cas " key " 10 1 6 " cas " noreply\r\nzzzzzz")) => ""
           (handle (str "get " key)) => (str "VALUE " key " 10 6\r\nzzzzzz\r\nEND\r\n")))))

 (fact-group
  :unit :incr-and-decr

  (fact "increment by 1 results in incremented value being returned"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 4\r\n1234")) => "STORED\r\n"
         (handle (str "incr " key " 1")) => "1235\r\n"))

  (fact "increment by number results in incremented value being returned"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 4\r\n1234")) => "STORED\r\n"
         (handle (str "incr " key " 10000")) => "11234\r\n"))

  (fact "increment by 1 when at max unsigned 64 bit number results in wrap around to zero (strangeness of the memcached spec!)"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 20\r\n18446744073709551615")) => "STORED\r\n"
         (handle (str "incr " key " 1")) => "0\r\n"))

  (fact "increment by number when close to max unsigned 64 bit number results in correct wrap around"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 20\r\n18446744073709551610")) => "STORED\r\n"
         (handle (str "incr " key " 10")) => "4\r\n"))

  (fact "decrement by 1 results in decremented value being returned"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 4\r\n1234")) => "STORED\r\n"
         (handle (str "decr " key " 1")) => "1233\r\n"))

  (fact "decrement by number results in decremented value being returned"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 4\r\n1234")) => "STORED\r\n"
         (handle (str "decr " key " 200")) => "1034\r\n"))

  (fact "decrement to below zero results in zero being returned (that's the way memcached wants it!)"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 4\r\n1234")) => "STORED\r\n"
         (handle (str "decr " key " 1235")) => "0\r\n"))

  (fact "decrement works at max limit"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 20\r\n18446744073709551615")) => "STORED\r\n"
         (handle (str "decr " key " 1")) => "18446744073709551614\r\n"))

  (fact "decrement fails when stored value is not a number"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 4\r\n12A4")) => "STORED\r\n"
         (handle (str "decr " key " 1")) => "CLIENT_ERROR stored value not a number\r\n"))

  (fact "decrement returns client error when the decrement value is not a number"
        (handle (str "decr key A")) => "CLIENT_ERROR\r\n")

  (fact "increment fails when stored value is not a number"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 4\r\n12A4")) => "STORED\r\n"
         (handle (str "incr " key " 1")) => "CLIENT_ERROR stored value not a number\r\n"))

  (fact "increment returns client error when the decrement value is not a number"
        (handle (str "incr key A")) => "CLIENT_ERROR\r\n")

  (fact "decrement fails when stored value can't be found"
        (let [key (uuid)]
          (handle (str "decr " key " 1")) => "NOT_FOUND\r\n"))

  (fact "increment fails when stored value can't be found"
        (let [key (uuid)]
          (handle (str "incr " key " 1")) => "NOT_FOUND\r\n"))

  (fact "decrement with noreply returns no response but does work"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 4\r\n1234")) => "STORED\r\n"
         (handle (str "decr " key " 200 noreply")) => ""
         (handle (str "get " key)) => (str "VALUE " key " 0 4\r\n1034\r\nEND\r\n")))

  (fact "increment with noreply returns no response but does work"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 4\r\n1234")) => "STORED\r\n"
         (handle (str "incr " key " 1 noreply")) => ""
         (handle (str "get " key)) => (str "VALUE " key " 0 4\r\n1235\r\nEND\r\n")))

  (fact "when decrement changes the size of the response the right changed size is returned by get"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 5\r\n10000")) => "STORED\r\n"
         (handle (str "decr " key " 9999 noreply")) => ""
         (handle (str "get " key)) => (str "VALUE " key " 0 1\r\n1\r\nEND\r\n")))

  (fact "when increment changes the size of the data the right changed size is returned by get"
        (let [key (uuid)]
         (handle (str "set " key " 0 300 3\r\n999")) => "STORED\r\n"
         (handle (str "incr " key " 1")) => "1000\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 4\r\n1000\r\nEND\r\n"))))

(fact-group
 :unit :touch

 (fact "touch alters ttl"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n")
         (handle (str "touch " key " 1")) => "TOUCHED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n")
         (Thread/sleep 1100)
         (handle (str "get " key)) => "END\r\n"))

 (fact "touch alters ttl when noreply is specified"
       (let [key (uuid)]
         (handle (str "set " key " 0 300 8\r\nsomedata")) => "STORED\r\n"
         (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n")
         (handle (str "touch " key " 1 noreply")) => ""
         (handle (str "get " key)) => (str "VALUE " key " 0 8\r\nsomedata\r\nEND\r\n")
         (Thread/sleep 1100)
         (handle (str "get " key)) => "END\r\n"))

 (fact "touch returns client error when the ttl value is not a number"
        (handle (str "touch key A")) => "CLIENT_ERROR\r\n")

  (fact "touch fails when stored value can't be found"
        (let [key (uuid)]
          (handle (str "touch " key " 1")) => "NOT_FOUND\r\n")))
