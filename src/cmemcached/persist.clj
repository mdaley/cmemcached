(ns cmemcached.persist
  (:require [cmemcached.util :refer [unsigned-sixty-four-bit-random]]
            [clojure.core.cache :as c]
            [crypto.random :as rnd]
            [environ.core :refer [env]]
            [pittlcache.core :as pc]))

(def ^:private default-ttl (env :default-ttl 60000))

(def cache (atom (pc/pittl-cache-factory {} :ttl default-ttl)))

(defn set-item
  [key flags ttl data]
  (swap! cache c/miss key {:value {:data data
                                   :flags flags
                                   :cas (unsigned-sixty-four-bit-random)}
                           :ttl ttl}))

(defn check-and-set
  [key flags ttl cas-unique data]
  (println "cas-unique" cas-unique)
  (if-let [existing (c/lookup @cache key)]
    (if (= cas-unique (:cas existing))
      (do (swap! cache c/miss key {:value {:data data
                                           :flags flags
                                           :cas (unsigned-sixty-four-bit-random)}
                                   :ttl ttl})
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
                                           :cas (unsigned-sixty-four-bit-random)}
                                   :ttl (:ttl existing)})
          :stored)
      :not-stored))

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
