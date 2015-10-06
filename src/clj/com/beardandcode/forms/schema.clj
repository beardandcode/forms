(ns com.beardandcode.forms.schema
  (:import [com.fasterxml.jackson.databind ObjectMapper]
           [com.github.fge.jackson.jsonpointer JsonPointer]
           [com.github.fge.jsonschema.core.load SchemaLoader]
           [com.github.fge.jsonschema.main JsonSchemaFactory]
           [com.beardandcode.forms SchemaWalker])
  (:require [clojure.java.io :as io]))


(defprotocol ISchema
  (as-map [_])
  (validate [_ instance]))

(defrecord Schema [tree schema loader]
  ISchema
  (as-map [_] (.walk (SchemaWalker. loader) tree))
  (validate [_ instance] false))

(defn new [path]
  (if-let [file (-> path io/resource io/file)]
    (let [mapper (ObjectMapper.)
          node (.readTree mapper file)
          factory (JsonSchemaFactory/byDefault)]
      (if (.schemaIsValid (.getSyntaxValidator factory) node)
        (let [loader (SchemaLoader.)
              tree (.setPointer (.load loader node) (JsonPointer/empty))
              schema (.getJsonSchema factory node)]
          (Schema. tree schema loader))))))
