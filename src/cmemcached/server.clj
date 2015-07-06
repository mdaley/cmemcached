(ns cmemcached.server
  (:require [cmemcached.handler :as handler]
            [aleph.tcp :as tcp]
            [byte-streams :as bytes]
            [clojure.tools.logging :as logging]
            [environ.core :refer [env]]
            [manifold.stream :as stream])
  (:import [java.util UUID]))

(def ^:private port (Integer/valueOf (env :port "11211")))

(defn- uuid
  []
  (str (UUID/randomUUID)))

(defn wrap-stream-handler
  "Returns a function that acts as the stream handler for each connection by a client.
  The parameter `f` is the function that will be called for each value passed in on
  the incoming part of the duplex stream between client and server. Each connection
  is given an unique id which passed to the function 'f' allowing tracking of the
  particular connection to be done (e.g. to allow statefulness)."
  [f]
  (fn [s info]
    (let [connectionid (uuid)]
      (logging/info (format "New connection %s - giving it a handler" connectionid))
      (stream/connect
       (stream/map #(f % connectionid info) s)
       s))))

(defn start-server
  "Start the server. The `stream-handler` is wrapped in a function that is passed to
  `tcp/start-server`. This function is called for each connection that is started between
  clients and the server."
  [stream-handler port]
  (tcp/start-server (fn [s info] (stream-handler s info))
                    {:port port}))

(defn -main [& args]
  (logging/info "Service starting")
  (start-server (wrap-stream-handler handler/handle-message) port))
