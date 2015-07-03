(ns cmemcached.handler
  (:require [byte-streams :as bytes]))

(defn handle-message
  [b]
  (println "MESSAGE:" (bytes/to-string b)))
