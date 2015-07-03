(ns cmemcached.acceptance.acceptance
  (:require [cmemcached.acceptance.acceptance :refer :all]
            [midje.sweet :refer :all]))

(fact-group
 :acceptance

 (fact "Something"
       1 => 1))
