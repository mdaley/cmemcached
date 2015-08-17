(ns cmemcached.acceptance.spyglass
  (:require [cmemcached.acceptance.common :refer :all]
            [clojurewerkz.spyglass.client :as spy]
            [environ.core :refer [env]]
            [midje.sweet :refer :all]))

(defn client [] (spy/text-connection (str "localhost:" port) (spy/text-connection-factory :failure-mode "retry")))

(defmacro with-retries
  [n body]
  `(loop [retries# ~n]
     (println "TRYING...")
     (if-let [result# (try
                       ~@body
                       (catch Exception e#
                         (when zero? retries#
                               (throw e#))))]
       (do (println "RESULT" result#)
           result#)
       (recur (dec retries#)))))

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
         (Thread/sleep 100)
         (spy/get (client) key) => value))

 (fact "async get works correctly"
       (let [key (uuid)
             value (uuid)
             ttl 300]
         (spy/set (client) key ttl value)
         (Thread/sleep 100)
         @(spy/async-get (client) key) => value))

 (fact "simple value can be set and retrieved but then it expires and can't be retrieved"
       (let [key (uuid)
             value (uuid)
             ttl 1]
         (spy/set (client) key ttl value)
         (Thread/sleep 100)
         (spy/get (client) key) => value
         (Thread/sleep 1100)
         (spy/get (client) key) => nil))

 (fact "several values can be set and all retrieved at the same time using the same client"
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
         (let [result (spy/get-multi c [key1 key2 key3])]
           result => {key1 value1 key2 value2 key3 value3})))

 (fact "add stores a value but subsequent add fails because the value is already present"
       (let [key (uuid)
             c (client)]
         @(spy/add c key 300 "abcd") => true
         @(spy/add c key 200 "anything") => false
         (spy/get c key) => "abcd"))

 (fact "replace fails when data doesn't already exist but does work once it does exist"
       (let [key (uuid)
             c (client)]
         @(spy/replace c key 300 "replacement") => false
         @(spy/set c key 300 "abcd") => true
         (spy/get c key) => "abcd"
         @(spy/replace c key 300 "replacement") => true
         (spy/get c key) => "replacement"))

 (fact "delete works correctly"
       (let [key (uuid)
             c (client)]
            @(spy/delete c key) => false
            @(spy/set c key 300 "abcd") => true
            (spy/get c key) => "abcd"
            @(spy/delete c key) => true
            (spy/get c key) => nil))

 ;; touch doesn't work with ASCII connection. Neither does get-and-touch.

 (fact "increment works correctly"
       (let [key (uuid)
             c (client)]
         (spy/set c key 300 "1")
         (spy/incr c key 1) => 2))

 (fact "decrement works correctly"
       (let [key (uuid)
             c (client)]
         (spy/set c key 300 "12")
         (spy/decr c key 10) => 2))

 (fact "gets and cas works correctly"
       (println "*************** GETS AND CAS ******************")
       (let [key (uuid)
             c (client)]
         @(spy/set c key 300 "abcd") => true
         (let [{:keys [cas value]} (spy/gets c key)]
              value => "abcd"
              cas => truthy
              (spy/cas c key 1234567890 "efgh") => :exists
              (spy/get c key) => "abcd"
              (spy/cas c key cas "efgh") => :ok
              (spy/get c key) => "efgh"
              )))
)
