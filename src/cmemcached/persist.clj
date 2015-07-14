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

(defn add-item
  [key flags ttl data]
  (if (c/has? @cache key)
    :no-stored
    (do (set-item key flags ttl data)
        :stored)))

(defn replace-item
  [key flags ttl data]
  (if (c/has? @cache key)
    (do (set-item key flags ttl data)
        :stored)
    :not-stored))

(defn retrieve-item
  [key]
  (let [result (c/lookup @cache key)]
    result))
