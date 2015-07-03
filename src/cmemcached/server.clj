(ns cmemcached.server
  (:require [cmemcached.handler :as h]
            [aleph.tcp :as tcp]
            [byte-streams :as bytes]
            [clojure.tools.logging :as logging]
            [environ.core :refer [env]]
            [manifold.stream :as s]))

(def ^:private port (env :port 11211))

(defn wrap-stream-handler
  "Returns a function that acts as the stream handler for each connection by a client.
  The parameter `f` is the function that will be called for each value passed in on
  the incoming part of the duplex stream between client and server."
  [f]
  (fn [s info]
    (logging/info "New connection - giving it a handler")
    (s/connect
     (s/map f s)
     s)))

(defn start-server
  "Start the server. The `stream-handler` is wrapped in a function that is passed to
  `tcp/start-server`. This function is called for each connection that is started between
  clients and the server."
  [stream-handler port]
  (tcp/start-server (fn [s info] (stream-handler s info))
                    {:port port}))

(defn -main [& args]
  (logging/info "Service starting")
  (start-server (wrap-stream-handler h/handle-message) port))
