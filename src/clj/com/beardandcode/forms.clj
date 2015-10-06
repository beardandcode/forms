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
