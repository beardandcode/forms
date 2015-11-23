(ns com.beardandcode.forms.schema-test
  (:require [clojure.test :refer :all]
            [com.beardandcode.forms.schema :as schema]))

(deftest test-new-schema
  (is (schema/new "schema/test.json"))
  (is (nil? (schema/new "schema/invalid.json")))
  (is (nil? (schema/new "schema/does-not-exist.json")))
  (is (nil? (schema/new "schema/invalid-bad-properties-and-attrs.json"))))
