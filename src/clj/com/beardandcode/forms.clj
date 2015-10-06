(ns com.beardandcode.forms
  (:require [com.beardandcode.forms.render :as render]
            [com.beardandcode.forms.schema :as schema]))

(defmacro defschema [symbol path]
  `(def ~symbol (schema/new ~path)))

(defn build
  ([action schema] (build action schema {}))
  ([action schema {:keys [method]
                   :or {method "POST"}}]
     (let [schema-map (schema/as-map schema)
           hiccup [:form {:action action :method method}
                   (render/schema schema-map)
                   [:input {:type "submit" :value (schema-map "submit")}]]]
       ;;(println hiccup)
       hiccup)))
