(ns cmemcached.cache
  (:require [environ.core :refer [env]]
            [pittlcache.core :refer [pittl-cache-factory]]))

(def ^:private default-ttl (env :default-ttl 60000))

(def ^:private cache
  (atom (pittl-cache-factory {} :ttl default-ttl)))
