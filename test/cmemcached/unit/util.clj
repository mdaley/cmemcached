(ns cmemcached.unit.util
  (:require [cmemcached.util :refer :all]
            [midje.sweet :refer :all]))

(def max-byte (bigint "255"))
(def max-unsigned-64 (bigint "18446744073709551615"))

(fact-group
 :unit

 (fact "When generating 64 bit unsigned in a bigint, the maximum value is correct"
       (unsigned-from-bytes (repeat 8 255N)) => max-unsigned-64)

 (fact "When generating 64 bit unsigned in a bigint, the minumum value is correct"
       (unsigned-from-bytes (repeat 8 0N)) => 0N)

 (fact "A mid range 64 bit unsigned value in a bigint is created correctly"
       (unsigned-from-bytes (repeat 8 41N)) => 2965947086361143593N)

 (fact "Another mid range 64 bit unsigned value in a bigint is created correctly"
       (unsigned-from-bytes (repeat 8 250N)) => 18085043209519168250N)

 (fact "max signed byte value gets converted to 0xFF"
       (unsigned-byte-value 0x7F) => 255N)

 (fact "min signed byte value gets convered to 0x00"
       (unsigned-byte-value -0x80) => 0N)

 (fact "Many random unsigned 64 bit numbers can be created and don't got out of range (at least in this test)."
       (let [result (reduce (fn [c n]
                              (vector (min n (first c))
                                      (max n (second c))))
                            [0N max-unsigned-64]
                            (take 1000000 (repeatedly unsigned-sixty-four-bit-random)))]
         (println result)
         (first result) => 0N
         (second result) => max-unsigned-64)))
