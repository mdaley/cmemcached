(ns cmemcached.handler
  (:require [cmemcached
             [version :as version]]
            [byte-streams :as bytes]
            [clojure.string :refer [split trim]]))

(defmulti handle-command
  (fn [message command] command))

(defmethod handle-command "version"
  [message action]
  (version/get-version))

(defmethod handle-command :default
  [_ _]
  "ERROR\r\n")

(defn handle-message
  [message-bytes]
  (let [message (bytes/to-string message-bytes)
        command (trim (first (split message #" ")))]
    (handle-command message command)))
