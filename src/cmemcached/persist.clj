(ns cmemcached.persist
  (:require [cmemcached.util :refer [unsigned-sixty-four-bit-random]]
            [clojure.core.cache :as c]
            [crypto.random :as rnd]
            [environ.core :refer [env]]
            [pittlcache.core :as pc]))

(def ^:private default-ttl (env :default-ttl 60000))

(def cache (atom (pc/pittl-cache-factory {} :ttl default-ttl)))

(defn store
  [key flags ttl data]
  (println "STORE" key flags ttl data)
  (if (c/has? @cache key)
    :exists
    (do
      (swap! cache c/miss key {:value {:data data
                                       :flags flags
                                       :cas (unsigned-sixty-four-bit-random)}
                               :ttl ttl})
      :stored)))

(defn retrieve
  [key]
  (let [result (c/lookup @cache key)]
    result))
