(ns cmemcached.persist
  (:require [cmemcached.util :refer [unsigned-sixty-four-bit-random max-unsigned-long
                                     positive-signed-sixty-four-bit-random]]
            [clojure.core.cache :as c]
            [crypto.random :as rnd]
            [environ.core :refer [env]]
            [pittlcache.core :as pc]))

(def ^:private max-unsigned-long-plus-one (inc max-unsigned-long))

;; In the memcached code - code as spec :-) - CAS is an unsigned 64 bit integer. However, many
;; client implementations limit the value to a Java long, a signed 64 bit integer (positive only).
;; So, this setting ensures that CAS values fit into this constrained range. In the standard
;; memcached implementation, successive CAS values increment from zero so there is unlikely to ever
;; be a problem. However, this implementation has random CAS values and hence their range needs to
;; be limited.
(def ^:private limit-cas-to-unsigned-long (Boolean/valueOf (env :limit-cas-to-unsigned-long true)))

(def ^:private default-ttl (env :default-ttl 60000))

(def cache (atom (pc/pittl-cache-factory {} :ttl default-ttl)))

(defn- random-cas
  []
  (if limit-cas-to-unsigned-long
    (positive-signed-sixty-four-bit-random)
    (unsigned-sixty-four-bit-random)))

(defn set-item
  [key flags ttl data]
  (swap! cache c/miss key {:value {:data data
                                   :flags flags
                                   :cas (random-cas)}
                           :ttl ttl}))

(defn check-and-set
  [key flags ttl cas-unique data]
  (println "CHECK-AND-SET" key flags ttl cas-unique data)
  (if-let [existing (c/lookup @cache key)]
    (if (= cas-unique (:cas existing))
      (do (swap! cache c/miss key {:value {:data data
                                           :flags flags
                                           :cas (random-cas)}
                                   :ttl ttl})
          (println "CAS STORED" (c/lookup @cache key))
          :stored)
      :exists)
    :not-found))

(defn add-item
  [key flags ttl data]
  (if (c/has? @cache key)
    :not-stored
    (do (set-item key flags ttl data)
        :stored)))

(defn replace-item
  [key flags ttl data]
  (if (c/has? @cache key)
    (do (set-item key flags ttl data)
        :stored)
    :not-stored))

(defn alter-item
  [key data append?]
  (if-let [existing (c/lookup @cache key)]
    (do (swap! cache c/miss key {:value {:data (if append?
                                                 (str (:data existing) data)
                                                 (str data (:data existing)))
                                         :flags (:flags existing)
                                         :cas (random-cas)}
                                 :ttl (:ttl existing)})
        :stored)
    :not-stored))

(defn touch
  [key ttl]
  (if-let [existing (c/lookup @cache key)]
    (do (swap! cache c/miss key {:value {:data (:data existing)
                                         :flags (:flags existing)
                                         :cas (random-cas)}
                                 :ttl ttl})
        :touched)
    :not-found))

(defn wrapping-increment
  [value increment]
  (str (mod (+ (bigint value) increment) max-unsigned-long-plus-one)))

(defn zeroing-decrement
  [value decrement]
  (let [amount (- (bigint value) decrement)]
    (if (< amount 0)
      "0"
      (str amount))))

(defn incr-or-decr
  [key increment modifier-fn]
  (if-let [existing (c/lookup @cache key)]
    (try
      (let [new-value (modifier-fn (:data existing) increment)]
          (swap! cache c/miss key {:value {:data new-value
                                           :flags (:flags existing)
                                           :cas (random-cas)}
                                   :ttl (:ttl existing)})
        [:stored new-value])
      (catch NumberFormatException e
        [:nan]))
    [:not-found]))

(defn increment
  [key increment]
  (incr-or-decr key increment wrapping-increment))

(defn decrement
  [key decrement]
  (incr-or-decr key decrement zeroing-decrement))

(defn retrieve-item
  [key]
  (let [result (c/lookup @cache key)]
    result))

(defn delete-item
  [key]
  (if (c/has? @cache key)
    (do (swap! cache c/evict key)
        :deleted)
    :not-found))
