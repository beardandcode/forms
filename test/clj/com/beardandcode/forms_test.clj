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
(forms/defschema invalid-order-schema "schema/invalid-order.json")
(forms/defschema nested-schema "schema/nested.json")

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
    (is (= (-> legend :content first) "Type"))
    (is (= (-> description :content first) "We have a couple of types available"))
    (is (= (count labels) (count inputs) 2))
    (is (every? #(= (-> % :attrs :name) "type") inputs))
    (is (every? (fn [[type elem]] (= (-> elem :attrs :value) type))
                (map vector ["free" "premium"] inputs)))
    (is (every? (fn [[type elem]] (= (-> elem :content rest first) type))
                (map vector ["Free" "Premium"] labels)))))

(deftest test-csrf-fn
  (let [form (->hickory [(forms/build "/" test-schema {:csrf-fn (fn [] ["<input type=\"hidden\" id=\"csrf\" />"])})])]
    (is (= (count (s/select (s/id "csrf") form)) 1))))

(deftest test-build-with-values
  (let [form (->hickory [(forms/build "/" test-schema {:values {"email-address" "foo@bar.com"
                                                                "type" "free"
                                                                "password" "foobles"}})])
        email-address (first (s/select (s/and (s/tag :input) (s/attr :name #(= % "email-address"))) form))
        types (s/select (s/and (s/tag :input) (s/attr :name #(= % "type"))) form)
        password (first (s/select (s/and (s/tag :input) (s/attr :name #(= % "password"))) form))]
    (is (= (-> email-address :attrs :value) "foo@bar.com"))
    (is (nil? (-> password :attrs :value)))                 ;; passwords should never be echoed
    (let [checked-types (filter #(not (nil? (-> % :attrs :checked))) types)]
      (is (= (count checked-types) 1))
      (is (= (-> checked-types first :attrs :value) "free")))))

(deftest test-build-with-errors
  (let [form (->hickory [(forms/build "/" test-schema {:errors {"email-address" [:invalid-email]
                                                                "type" [:required :some-other-error]
                                                                "password" [:required]}
                                                       :error-text-fn #(str %3)})])
        email (first (s/select (s/id "email-address") form))
        email-errors (s/select (s/and (s/tag :p) (s/class "error")) email)
        type (first (s/select (s/id "type") form))
        type-errors (s/select (s/and (s/tag :p) (s/class "error")) type)
        password (first (s/select (s/id "password") form))
        password-errors (s/select (s/and (s/tag :p) (s/class "error")) password)
        repeat-password (first (s/select (s/id "repeat-password") form))
        repeat-password-errors (s/select (s/and (s/tag :p) (s/class "error")) repeat-password)]
    (is (= (-> email :attrs :class) "error"))
    (is (= (count email-errors) 1))
    (is (= (-> email-errors first :content first) ":invalid-email"))
    (is (= (-> type :attrs :class) "error"))
    (is (= (count type-errors) 2))
    (is (= (-> type-errors first :content first) ":required"))
    (is (= (-> type-errors second :content first) ":some-other-error"))
    (is (= (-> password :attrs :class) "error"))
    (is (= (count password-errors) 1))
    (is (= (-> password-errors first :content first) ":required"))
    (is (= (-> repeat-password :attrs :class) ""))
    (is (= (count repeat-password-errors) 0))))

(deftest test-build-invalid-order
  (let [form (->hickory [(forms/build "/" invalid-order-schema)])]
    (is form)
    (is (= (count (s/select (s/tag :label) form)) 1))))

(deftest test-build-nested
  (let [form (->hickory [(forms/build "/" nested-schema)])
        address-fieldset (first (s/select (s/id "address") form))
        address-fields (s/select (s/descendant (s/id "address") (s/tag :input)) form)]
    (is address-fieldset)
    (is (= (-> address-fieldset :attrs :id) "address"))
    (is (= (count address-fields) 3))
    (is (= (-> address-fields first :attrs :name) "address_line-1"))))







(deftest test-values-nested
  (let [values (forms/values {:form-params {"address_line-1" "5 Foo Street"
                                            "shouldnt-be-there" "it is"
                                            "name" "Mr Bar"}}
                             nested-schema)]
    (is (= "Mr Bar" (values "name")))
    (is (= "5 Foo Street" (get-in values ["address" "line-1"])))
    (is (nil? (values "shouldnt-be-there")))))

(deftest test-nuking-empty-strings
  (let [values (forms/values {:form-params {"name" ""
                                            "address_line-1" ""}}
                             nested-schema)]
    (is (not ((set (keys values)) "name")))
    (is (not ((set (keys values)) "address")))))








(deftest test-errors-missing-params
  (let [request {:form-params {}}
        errors (forms/errors request test-schema)]
    (is (= (-> errors keys count) 3))
    (is (some #(= % :required) (errors "email-address")))
    (is (some #(= % :required) (errors "password")))
    (is (some #(= % :required) (errors "repeat-password")))))

(deftest test-errors-empty-params
  (let [request {:form-params {"email-address" ""
                               "password" ""
                               "repeat-password" ""}}
        errors (forms/errors request test-schema)]
    (is (= (-> errors keys count) 3))
    (is (some #(= % :required) (errors "email-address")))
    (is (some #(= % :required) (errors "password")))
    (is (some #(= % :required) (errors "repeat-password")))))

(deftest test-errors-when-bad-email
  (let [request {:form-params {"email-address" "foobar.com"
                               "password" "asdf"
                               "repeat-password" "asdf"}}
        errors (forms/errors request test-schema)]
    (is (= (-> errors keys count) 1))
    (is (some #(= % :invalid-email) (errors "email-address")))))

(deftest test-errors-when-valid
  (let [request {:form-params {"email-address" "foo@bar.com"
                               "password" "asdf"
                               "repeat-password" "asdf"}}]
    (is (nil? (forms/errors request test-schema)))))

(deftest test-errors-trims-csrf-field
  (let [request {:form-params {"csrf-field" "some-value"
                               "email-address" "foo@bar.com"
                               "password" "asdf"
                               "repeat-password" "asdf"}}]
    (is (nil? (forms/errors request test-schema {:csrf-field "csrf-field"})))))

(deftest test-errors-goes-deep
  (let [request {:form-params {"email-address" "asdf"}}
        errors (forms/errors request test-schema)]
    (is (= (-> errors keys count) 3))
    (is (some #(= % :invalid-email) (errors "email-address")))
    (is (some #(= % :required) (errors "password")))
    (is (some #(= % :required) (errors "repeat-password")))))
