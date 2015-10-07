(ns com.beardandcode.forms
  (:require [com.beardandcode.forms.render :as render]
            [com.beardandcode.forms.schema :as schema]))

(defmacro defschema [symbol path]
  `(def ~symbol (schema/new ~path)))

(defn build
  ([action schema] (build action schema {}))
  ([action schema {:keys [method csrf-fn]
                   :or {method "POST"
                        csrf-fn (fn [& _] '())}}]
     (let [schema-map (schema/as-map schema)
           hiccup [:form {:action action :method method}
                   (concat '() (csrf-fn)
                           (render/schema schema-map)
                           (list [:input {:type "submit" :value (schema-map "submit")}]))]]
       hiccup)))

(defn errors
  ([request schema] (errors request schema {}))
  ([request schema {:keys [csrf-field]
                    :or {csrf-field "__anti-forgery-token"}}]
   (let [params (->> (:form-params request)
                     (filter (fn [[key value]] (not (or (= key csrf-field) (empty? value)))))
                     flatten
                     (apply hash-map))]
     (schema/validate schema params))))
