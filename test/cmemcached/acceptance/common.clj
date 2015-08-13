(ns cmemcached.acceptance.common
  (:require [environ.core :refer [env]]
            [midje.sweet :refer :all])
  (:import [java.util UUID]))

(def port (Integer/valueOf (env :port 11211)))

(defn uuid
  []
  (str (UUID/randomUUID)))
