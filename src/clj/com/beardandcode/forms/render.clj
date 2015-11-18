(ns com.beardandcode.forms.render
  (:require [clojure.string :as s]
            [clojure.set :refer [difference]]))

(defn- pick-title [name details]
  (or (details "title") (s/capitalize (s/replace name #"[-_]" " "))))

(defn- password? [details]
  (= (details "format") "password"))

(defn- sort-properties [schema-properties ordered-names]
  (let [all-names (set (keys schema-properties))
        unordered-names (difference all-names ordered-names)
        pick #(vector % (schema-properties %))]
    (concat (map pick ordered-names)
            (map pick unordered-names))))



(defmulti render-property (fn [_ details _ _] (details "type")))

(defmethod render-property "string" [name details value errors]
  [:label {:class (if (> (count errors) 0) "error" "") :id name} (pick-title name details)
   (concat (if (details "description") (list [:p (details "description")]) '())
           (if (> (count errors) 0) (map #(vector :p {:class "error"} %) errors) '())
           (list [:input {:type (if (password? details) "password" "text")
                          :name name
                          :value (if (password? details) nil value)}]))])

(defmethod render-property nil [name details value errors]
  (if-let [enum (details "enum")]
    [:fieldset {:class (if (> (count errors) 0) "error" "") :id name}
     (concat (list [:legend (pick-title name details)])
             (if (details "description") (list [:p (details "description")]) '())
             (if (> (count errors) 0) (map #(vector :p {:class "error"} %) errors) '())
              (map #(vector :label
                            (let [input-attrs {:type "radio" :value % :name name}]
                              [:input (if (= value %) (assoc input-attrs :checked "checked") input-attrs)])
                            (s/capitalize %)) enum))]))

(defmethod render-property :default [& args]
  (println args))



(defn schema [schema-map values errors]
  (map (fn [[name details]]
         (render-property name details (get values name "") (get errors name [])))
       (sort-properties (schema-map "properties")
                        (schema-map "order"))))
