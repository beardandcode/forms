(ns com.beardandcode.forms.render
  (:require [clojure.string :as s]
            [clojure.set :refer [difference]]))

(defn- pick-title [name details]
  (or (details "title") (s/capitalize (s/replace name #"[-_]" " "))))

(defn- sort-properties [schema-properties ordered-names]
  (let [all-names (set (keys schema-properties))
        unordered-names (difference all-names ordered-names)
        pick #(vector % (schema-properties %))]
    (concat (map pick ordered-names)
            (map pick unordered-names))))

(defmulti render-property (fn [[name details]] (details "type")))
(defmethod render-property "string" [[name details]]
  [:label (pick-title name details)
   (conj (if (details "description") (list [:p (details "description")]) '())
         [:input {:type (if (= (details "format") "password") "password" "text")
                  :name name}])])
(defmethod render-property nil [[name details]]
  (if-let [enum (details "enum")]
    [:fieldset (concat (list [:legend (pick-title name details)])
                       (if (details "description") (list [:p (details "description")]) '())
                       (map #(vector :label [:input {:type "radio" :value % :name name}] (s/capitalize %)) enum))]))
(defmethod render-property :default [thing]
  (println thing))

(defn schema [schema-map]
  (map #(render-property %) (sort-properties (schema-map "properties")
                                             (schema-map "order"))))
