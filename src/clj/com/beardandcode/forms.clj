(ns com.beardandcode.forms
  (:require [com.beardandcode.forms.render :as render]
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
                           (render/schema schema-map values error-text)
                           (list [:input {:type "submit" :value (schema-map "submit")}]))]]
       hiccup)))

(defn values [request]
  (:form-params request))

(defn errors
  ([request schema] (errors request schema {}))
  ([request schema {:keys [csrf-field]
                    :or {csrf-field "__anti-forgery-token"}}]
   (let [params (->> (values request)
                     (filter (fn [[key value]] (not (or (= key csrf-field) (empty? value)))))
                     flatten
                     (apply hash-map))]
     (schema/validate schema params))))
