(ns cmemcached.acceptance.common
  (:require [clojurewerkz.spyglass.client :as spy]
            [environ.core :refer [env]]
            [midje.sweet :refer :all])
  (:import [java.util UUID]))

(def port (env :port 11211))

(def client (memoize #(spy/text-connection (str "localhost:" port))))

(defn uuid
  []
  (str (UUID/randomUUID)))
