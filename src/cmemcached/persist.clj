(ns jmemcached.persist
  (:require [environ.core :refer [env]]
            [pittlcache.core :as cache]))

(def ^:private default-ttl (env :default-ttl 60000))

(def cache (atom (cache/pittl-cache-factory {} :ttl default-ttl)))

(defn store
  [key flags ttl value]
  ())
