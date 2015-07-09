(ns cmemcached.persist
  (:require [clojure.core.cache :as c]
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
      (swap! cache c/miss key {:value {:data data :flags flags} :ttl ttl})
      :stored)))

(defn retrieve
  [key]
  (c/lookup @cache key))
