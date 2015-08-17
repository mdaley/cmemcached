(defproject cmemcached "0.1.0-SNAPSHOT"
  :description "A simple in-memory implementation of memcached"
  :url "http://github.com/mdaley/cmemcached"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[aleph "0.4.0"]
                 [byte-streams "0.2.0"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [crypto-random "1.2.0"]
                 [environ "1.0.0"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [pittlcache "0.1.0"]]

  :main cmemcached.server

  :profiles {:dev {:dependencies [[net.spy/spymemcached "2.12.0"]
                                  [clojurewerkz/spyglass "1.1.0"
                                   :exclusions [net.spy/spymemcached]]
                                  [clj-xmemcached "0.2.6-RC1"]
                                  [midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]]}
             ;; Need separate profile for elasticache tests because it uses same net.spy.memcached
             ;; namespaces as the library wrapped by spyglass. Running in the same profile would
             ;; cause a conflict with potentially the wrong implementation being used for tests!
             :elc {:dependencies [[com.amazonaws/elasticache-java-cluster-client "1.0.61.0"]
                                  [midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]]}}

  :env {:port 11211
        :default-ttl 2000
        :limit-cas-to-signed-long true
        :elasticache-auto-discovery true}
)
