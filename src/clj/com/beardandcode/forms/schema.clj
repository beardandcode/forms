(ns com.beardandcode.forms.schema
  (:import [com.beardandcode.forms Schema])
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]))

(defprotocol ISchema
  (as-map [_])
  (validate [_ instance]))

(defn- pointer [error & suffixes]
  (let [parts (filter #(not (or (empty? %) (= "properties" %)))
                      (clojure.string/split ((error "instance") "pointer") #"/"))]
    (str "/" (clojure.string/join "/" (concat parts suffixes)))))

(extend Schema
  ISchema
  {:as-map #(.asMap %)
   :validate (fn [s instance]
               (let [raw-errors (json/parse-string (.validate s (json/generate-string instance)))]
                 (if (not (empty? raw-errors))
                   (->> raw-errors
                        (map (fn [error] (cond
                                          (= (error "keyword") "required")
                                          (map #(list (pointer error %) :required) (error "missing"))

                                          (= (error "keyword") "format")
                                          [[(pointer error)
                                            (keyword (str "invalid-" (error "attribute")))]]

                                          :else
                                          [])))
                        (reduce (fn [error-map errors]
                                  (reduce (fn [error-map [field error]]
                                            (assoc error-map field (if-let [other-errors (error-map field)]
                                                                     (conj other-errors error)
                                                                     [error])))
                                          error-map errors))
                                {})))))})

(defn new [path]
  (if-let [schema-url (io/resource path)]
    (let [schema (Schema. (slurp schema-url))]
      (if (.isValid schema) schema))))
