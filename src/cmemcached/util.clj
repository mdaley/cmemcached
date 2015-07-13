(ns cmemcached.util
  (:require [crypto.random :as rnd]))

(defn unsigned-from-bytes
  "Turn a sequence of bigints representing byte values into the number they represent. For example
  the sequence `(255N 255N 255N 255N 255N 255N 255N 255N)` gets turned into the maximum value of
  an unsigned 64 bit integer."
  [bigints]
  (first (reduce (fn [n b]
                   (vector (+ (first n) (* (second n) b))
                           (* 256N (second n))))
                 (vector 0N 1N)
                 bigints)))

(defn unsigned-byte-value
  "Turn a clojure signed byte into an unsigned value between 0 and 255 inclusive"
  [b]
  (+ 128N (bigint b)))

(defn unsigned-sixty-four-bit-random
  "Create an unsigned 64 bit random number (in a bigint because it won't fit in a signed long!)."
  []
  (->> (rnd/bytes 8)
       (map unsigned-byte-value)
       (unsigned-from-bytes)))
