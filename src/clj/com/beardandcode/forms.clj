(ns com.beardandcode.forms
  (:import [com.fasterxml.jackson.databind ObjectMapper]
           [com.github.fge.jackson.jsonpointer JsonPointer]
           [com.github.fge.jsonschema.core.load SchemaLoader]
           [com.github.fge.jsonschema.main JsonSchemaFactory]
           [com.beardandcode.forms SchemaWalker])
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [clojure.set :refer [difference]]))

(defprotocol ISchema
  (as-map [_])
  (validate [_ instance]))

(defrecord Schema [tree schema loader]
  ISchema
  (as-map [_] (.walk (SchemaWalker. loader) tree))
  (validate [_ instance] false))

(defn new-schema [path]
  (if-let [file (-> path io/resource io/file)]
    (let [mapper (ObjectMapper.)
          node (.readTree mapper file)
          factory (JsonSchemaFactory/byDefault)]
      (if (.schemaIsValid (.getSyntaxValidator factory) node)
        (let [loader (SchemaLoader.)
              tree (.setPointer (.load loader node) (JsonPointer/empty))
              schema (.getJsonSchema factory node)]
          (Schema. tree schema loader))))))

(defmacro defschema [symbol path]
  `(def ~symbol (new-schema ~path)))

(defn pick-title [name details]
  (or (details "title") (s/capitalize (s/replace name #"[-_]" " "))))

(defmulti build-property (fn [[name details]] (details "type")))
(defmethod build-property "string" [[name details]]
  [:label (pick-title name details)
   (conj (if (details "description") (list [:p (details "description")]) '())
         [:input {:type (if (= (details "format") "password") "password" "text")
                  :name name}])])
(defmethod build-property nil [[name details]]
  (if-let [enum (details "enum")]
    [:fieldset (concat (list [:legend (pick-title name details)])
                       (if (details "description") (list [:p (details "description")]) '())
                       (map #(vector :label [:input {:type "radio" :value % :name name}] (s/capitalize %)) enum))]))
(defmethod build-property :default [thing]
  (println thing))

(defmacro print-thru [body]
  `(let [~'result ~body]
     (println ~'result)
     ~'result))

(defn sort-properties [schema-properties ordered-names]
  (let [all-names (set (keys schema-properties))
        unordered-names (difference all-names ordered-names)
        pick #(vector % (schema-properties %))]
    (concat (map pick ordered-names)
            (map pick unordered-names))))

(defn build
  ([action schema] (build action schema {}))
  ([action schema {:keys [method]
                   :or {method "POST"}}]
     (let [schema-map (as-map schema)
           hiccup [:form {:action action :method method}
                   (map #(build-property %) (sort-properties (schema-map "properties")
                                                             (schema-map "order")))
                   [:input {:type "submit" :value (schema-map "submit")}]]]
       ;;(println hiccup)
       hiccup)))
