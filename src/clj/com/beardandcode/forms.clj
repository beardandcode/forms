(ns com.beardandcode.forms
  (:require [hiccup.core :refer [html]]
            [com.beardandcode.forms.render :as render]
            [com.beardandcode.forms.schema :as schema]
            [com.beardandcode.forms.text :as text]))

(defmacro defschema [symbol path]
  `(def ~symbol (schema/new ~path)))

(defn build
  ([action schema] (build action schema {}))
  ([action schema {:keys [errors values method csrf-fn error-text-fn]
                   :or {errors {}
                        values {}
                        method "POST"
                        csrf-fn (fn [& _] '())
                        error-text-fn (fn [_ _ error] (get text/english error "Unknown error"))}}]
     (let [schema-map (schema/as-map schema)
           error-text (reduce (fn [error-text [field field-errors]]
                                 (assoc error-text field (map #(error-text-fn schema field %) field-errors)))
                              {} errors)
           hiccup [:form {:action action :method method}
                   (concat '() (csrf-fn)
                           (render/error-list (error-text "/" []))
                           (render/schema schema-map values error-text)
                           (list [:input {:type "submit" :value (schema-map "submit")}]))]]
       (html hiccup))))

(defn- with-prefix [n prefix]
  (if (empty? prefix)
    n (str prefix "_" n)))

(defn- values-using-schema [data schema-map prefix]
  (reduce (fn [out-map [name detail]]
            (let [name-with-prefix (with-prefix name prefix)]
              (if (= (detail "type") "object")
                (assoc out-map name (values-using-schema data detail name-with-prefix))
                (if (and (contains? data name-with-prefix)
                         (not (empty? (data name-with-prefix))))
                  (assoc out-map name (data name-with-prefix))
                  out-map))))
          {} (schema-map "properties")))

(defn values [request schema]
  (values-using-schema (:form-params request) (schema/as-map schema) ""))

(defn errors
  ([request schema] (errors request schema {}))
  ([request schema {:keys [csrf-field]
                    :or {csrf-field "__anti-forgery-token"}}]
   (let [params (->> (values request schema)
                     (filter (fn [[key value]] (not (= key csrf-field))))
                     flatten
                     (apply hash-map))]
     (schema/validate schema params))))
