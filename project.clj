(defproject cmemcached "0.1.0-SNAPSHOT"
  :description "A simple in-memory implementation of memcached"
  :url "http://github.com/mdaley/cmemcached"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[aleph "0.4.0"]
                 [byte-streams "0.2.0"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [environ "1.0.0"]
                 [org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.1"]
                 [pittlcache "0.1.0"]]

  :env {:port 11211
        :default-ttl 2000}

  :main cmemcached.server

  :profiles {:dev {:dependencies [[clojurewerkz/spyglass "1.0.0"]
                                  [midje "1.6.3"]]
                   :plugins [[lein-midje "3.1.3"]]}})
