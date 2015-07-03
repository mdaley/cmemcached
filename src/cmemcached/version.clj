(ns cmemcached.version
  (:require [clojure.java.io :as io])
  (:import [java.io Reader]
           [java.util Properties]))

(defn- read-file-to-properties
  [file-name]
  (with-open [^Reader reader (io/reader file-name)]
    (let [props (Properties.)]
      (.load props reader)
      (into {} (for [[k v] props] [k v])))))

(def ^:private version
  (memoize
   (fn []
     (let [pom-path (format "META-INF/maven/cmemcached/cmemcached/pom.properties")]
       (if-let [path (.getResource (ClassLoader/getSystemClassLoader) pom-path)]
         ((read-file-to-properties path) "version")
         "development")))))

(defn get-version
  []
  (str "VERSION " (version) "\r\n"))
