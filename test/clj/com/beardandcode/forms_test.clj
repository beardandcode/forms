(ns com.beardandcode.forms-test
  (:require [clojure.test :refer :all]
            [hickory.convert :refer [hiccup-to-hickory]
             :rename {hiccup-to-hickory ->hickory}]
            [hickory.select :as s]
            [hickory.render :refer [hickory-to-html]]
            [com.beardandcode.forms :as forms]))

(forms/defschema test-schema "schema/test.json")
(forms/defschema no-title-schema "schema/no-title.json")
(forms/defschema description-schema "schema/description.json")
(forms/defschema enum-schema "schema/enum.json")

(deftest test-build-basics
  (let [form (->hickory [(forms/build "/endpoint" test-schema)])
        form-elem (first (s/select (s/tag :form) form))
        submit-button (first (s/select (s/and (s/tag :input) (s/attr :type #(= % "submit"))) form))]
    (is (= (:tag form-elem) :form))
    (is (= (-> form-elem :attrs :action) "/endpoint"))
    (is (= (-> form-elem :attrs :method)) "POST")
    (is submit-button)))

(deftest test-build-method
  (let [form (->hickory [(forms/build "/" test-schema {:method "PUT"})])
        form-elem (first (s/select (s/tag :form) form))]
    (is (= (-> form-elem :attrs :method) "PUT"))))

(deftest test-build-from-schema
  (let [hiccup (forms/build "/" test-schema)
        form (->hickory [hiccup])
        inputs (s/select (s/child (s/tag :label) (s/tag :input)) form)
        password-inputs (s/select (s/and (s/tag :input) (s/attr :type #(= % "password"))) form)
        submit-button (first (s/select (s/and (s/tag :input) (s/attr :type #(= % "submit"))) form))]
    (spit "/tmp/schema-form.html" (hickory-to-html form))
    (is (= (count inputs) 6))              ;; five properties but one is an enum
    (is (= (count password-inputs) 2))     ;; so should be two radio inputs
    (is (= (mapv #(-> % :attrs :name) inputs)
           ["email-address" "name" "type" "type" "password" "repeat-password"]))
    (is (= (-> submit-button :attrs :value) "Register"))))

(deftest test-build-property-no-title
  (let [form (->hickory [(forms/build "/" no-title-schema)])
        label (first (s/select (s/tag :label) form))]
    (is (= (-> label :content first) "Some field"))))

(deftest test-build-property-input-name
  (let [form (->hickory [(forms/build "/" no-title-schema)])
        input (first (s/select (s/tag :input) form))]
    (is (= (-> input :attrs :name) "some-field"))))

(deftest test-build-property-description
  (let [form (->hickory [(forms/build "/" description-schema)])
        label (first (s/select (s/tag :label) form))
        description (first (s/select (s/child (s/tag :label) (s/tag :p)) form))]
    (is (= (-> label :content first) "Your email address"))
    (is (= (-> description :content first) "An email address"))))

(deftest test-build-property-enum
  (let [form (->hickory [(forms/build "/" enum-schema)])
        labels (s/select (s/tag :label) form)
        inputs (s/select (s/and (s/tag :input) (s/attr :type #(= % "radio"))) form)
        legend (first (s/select (s/child (s/tag :fieldset) (s/tag :legend)) form))
        description (first (s/select (s/child (s/tag :fieldset) (s/tag :p)) form))]
    (spit "/tmp/schema-form-enum.html" (hickory-to-html form))
    (is (= (-> legend :content first) "Type"))
    (is (= (-> description :content first) "We have a couple of types available"))
    (is (= (count labels) (count inputs) 2))
    (is (every? #(= (-> % :attrs :name) "type") inputs))
    (is (every? (fn [[type elem]] (= (-> elem :attrs :value) type))
                (map vector ["free" "premium"] inputs)))
    (is (every? (fn [[type elem]] (= (-> elem :content rest first) type))
                (map vector ["Free" "Premium"] labels)))))
