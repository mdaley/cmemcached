(ns cmemcached.handler
  (:require [cmemcached
             [version :as version]]
            [byte-streams :as bytes]
            [clojure.string :refer [split trim]]))

(def connection-cmd (atom {}))

(defn- clear-connection-cmd!
  [connectionid]
  (swap! connection-cmd
         (fn [curr id]
           (assoc curr id nil))
         connectionid))

(defn- decode-command
  [message])

(defmulti handle-command
  (fn [connectionid message command] command))

(defmethod handle-command "version"
  [connectionid message action]
  (version/get-version))

(defmethod handle-command "set"
  [connectionid message action]
  (println "SET")
  (swap! connection-cmd
         (fn [curr id]
           (assoc curr id {:msg {:message message} :cmd "complete-set"}))
         ;; (fn [curr id]
         ;;   (update-in curr [id] {:msg message :cmd "complete-set"}))
         connectionid)
  nil)

(defmethod handle-command "complete-set"
  [connectionid message action]
  (println "COMPLETE-SET"))

(defmethod handle-command :default
  [_ _ _]
  "ERROR\r\n")

(defn handle-message
  [message-bytes connectionid info]
  (println "HANDLE-MESSAGE" connectionid info)
  (let [deferred (get @connection-cmd connectionid)]
    (println "DEFERRED" deferred)
    (if deferred
      (do
        (clear-connection-cmd! connectionid)
        (handle-command connectionid (assoc (:msg deferred) :data message-bytes) (:cmd deferred)))
      (let [message (bytes/to-string message-bytes)]
        (handle-command connectionid message (trim (first (split message #"\s+"))))))))
