(ns cmemcached.handler
  (:require [cmemcached
             [persist :as persist]
             [version :as version]]
            [byte-streams :as bytes]
            [clojure.string :refer [split trim]]))

(def ^:const max-unsigned-int 4294967295N)
(def ^:const max-unsigned-long 18446744073709551615N)

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
                         (bigint cas)))
          noreply (nth params (if (= "cas" cmd) 5 4) nil)]
      (when (and (<= 0 flags max-unsigned-int)
                 (<= 0 exptime)
                 (<= 0 bytes)
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
  (fn [connectionid msg data cmd] cmd))

(defmethod handle-command "version"
  [connectionid _ _ _]
  (version/get-version))

(defmethod handle-command "set"
  [connectionid msg data cmd]
  (if-let [params (decode-params msg cmd)]
    (if (= (count data) (:bytes params))
      (if (= :stored (persist/store (:key params) (:flags params) (* (:exptime params) 1000) data))
        "STORED\r\n"
        "EXISTS\r\n")
      "CLIENT_ERROR\r\n")
    "CLIENT_ERROR\r\n"))

(defn- retrieve-item
  [key with-cas]
  (when-let [result (persist/retrieve key)]
    (format "VALUE %s %s %s%s\r\n%s\r\n"
            key
            (:flags result)
            (count (:data result))
            (if with-cas (str " " (:cas result)) "")
            (str (:data result)))))

(defmethod handle-command "get"
  [connectionid message _ _]
  (if (seq message)
    (str (reduce (fn [s key] (str s (retrieve-item key false))) "" message) "END\r\n")
    "CLIENT_ERROR\r\n"))

(defmethod handle-command "gets"
  [connectionid message _ _]
  (if (seq message)
    (str (reduce (fn [s key] (str s (retrieve-item key true))) "" message) "END\r\n")))

(defmethod handle-command "cas"
  [connectionid message data cmd]
  (if-let [params (decode-params message cmd)]
    "DO SOMETHING\r\n"
    "CLIENT_ERROR\r\n"))

(defmethod handle-command :default
  [_ _ _ _]
  "ERROR\r\n")

(defn handle-message
  [message-bytes connectionid info]
  (bytes/print-bytes message-bytes)
  (let [message (bytes/to-string message-bytes)
        lines (split message #"\r\n")
        cmd-and-args (when (first lines) (split (first lines) #"\s+"))
        cmd (first cmd-and-args)
        args (rest cmd-and-args)
        data (second lines)]
    (handle-command connectionid args data cmd)))
