(ns cmemcached.handler
  (:require [cmemcached
             [version :as version]]
            [byte-streams :as bytes]
            [clojure.string :refer [split trim]]))

(def ^:const max-unsigned-int 4294967295)
(def ^:const max-unsigned-long (BigInteger. "18446744073709551615"))

(def deferred-cmd (atom {}))

(defn- clear-connection-cmd!
  [connectionid]
  (swap! deferred-cmd
         (fn [curr id]
           (assoc curr id nil))
         connectionid))

(defn- set-deferred-cmd!
  [connectionid message cmd]
  (println "SET DEFERRED" connectionid message cmd)
  (swap! deferred-cmd
         (fn [curr id]
           (assoc curr id {:msg {:message message} :cmd cmd}))
         connectionid))

;; NOTE: exptime is in seconds if it is less than 30 days, otherwise it is a unix timestamp!! Need exact definition.

(defn- decode-params
  [params cmd]
  (try
    (let [key (first params)
          flags (Long/valueOf (second params))
          exptime (Long/valueOf (nth params 2 nil))
          bytes (Long/valueOf (nth params 3 nil))
          cas-unique (when (= "cas" cmd)
                       (when-let [cas (nth params 4 nil)]
                         (BigInteger. cas)))
          noreply (nth params (if (= "cas" cmd) 5 4) nil)]
      (when (and (<= 0 flags max-unsigned-int)
                 (<= 0 exptime)
                 (<= 1 bytes)
                 (or (nil? cas-unique) (<= 0 cas-unique max-unsigned-long))
                 (or (nil? noreply) (= "noreply" noreply)))
        (merge {:key key
                :flags flags
                :exptime exptime
                :bytes bytes}
               (when cas-unique
                 {:cas-unique cas-unique})
               (when noreply
                 {:noreply true}))))
    (catch NumberFormatException e)))

(defmulti handle-command
  (fn [connectionid message cmd] cmd))

(defmethod handle-command "version"
  [connectionid message cmd]
  (version/get-version))

(defmethod handle-command "set"
  [connectionid message cmd]
  (println "SET")
  (if-let [params (decode-params message cmd)]
    (do
      (set-deferred-cmd! connectionid params "complete-set")
      nil)
    "CLIENT_ERROR\r\n"))

(defmethod handle-command "cas"
  [connectionid message cmd]
  (println "CAS")
  (if-let [params (decode-params message cmd)]
    (do
      (set-deferred-cmd! connectionid params "complete-cas")
      nil)
    "CLIENT_ERROR\r\n"))

(defmethod handle-command "complete-set"
  [connectionid message cmd]
  (println "COMPLETE-SET" message cmd))

(defmethod handle-command :default
  [_ _ _]
  "ERROR\r\n")

(defn handle-message
  [message-bytes connectionid info]
  (println "HANDLE MESSAGE" connectionid info)
  (let [deferred (get @deferred-cmd connectionid)]
    (println "DEFERRED" deferred)
    (if deferred
      (do
        (clear-connection-cmd! connectionid)
        (handle-command connectionid (assoc (:msg deferred) :data message-bytes) (:cmd deferred)))
      (let [message (bytes/to-string message-bytes)
            parts (split message #"\s+")]
        (handle-command connectionid (rest parts) (first parts))))))
