(ns com.beardandcode.forms.render
  (:require [clojure.string :as s]
            [clojure.set :refer [difference]]))

(defn- pick-title [name details]
  (or (details "title") (s/capitalize (s/replace name #"[-_]" " "))))

(defn- password? [details]
  (= (details "format") "password"))

(defn- sort-properties [schema-properties ordered-names-raw]
  (let [all-names (set (keys schema-properties))
        ordered-names (filter all-names ordered-names-raw)
        unordered-names (difference all-names ordered-names)
        pick #(vector % (schema-properties %))]
    (concat (map pick ordered-names)
                    (map pick unordered-names))))


(defmulti render-property (fn [_ details _ _] (details "type")))


(defn schema
  ([schema-map values errors] (schema schema-map values errors []))
  ([schema-map values errors prefixes]
    (map (fn [[name details]]
           (render-property (conj prefixes name) details values errors))
         (sort-properties (schema-map "properties")
                          (schema-map "order")))))


(defn- find-value [values path]
  (get-in values path))

(defn- find-errors [errors path]
  (errors (str "/" (clojure.string/join "/" path))))

(defn- as-id [path] (clojure.string/join "_" path))
(defn- as-name [path] (last path))


(defmethod render-property "string" [path details values errors]
  (let [prop-errors (find-errors errors path)
        id (as-id path)
        name (as-name path)]
    [:label {:class (if (> (count prop-errors) 0) "error" "") :id id} (pick-title name details)
     (concat (if (details "description") (list [:p (details "description")]) '())
             (if (> (count prop-errors) 0) (map #(vector :p {:class "error"} %) prop-errors) '())
             (list [:input {:type (if (password? details) "password" "text")
                            :name id
                            :value (if (password? details) nil (find-value values path))}]))]))

(defmethod render-property "object" [path details values errors]
  [:fieldset {:id (as-id path)}
   (concat (list [:legend (pick-title (as-name path) details)])
           (schema details values errors path))])

(defmethod render-property nil [path details values errors]
  (let [prop-errors (find-errors errors path)
        id (as-id path) name (as-name path)]
    (if-let [enum (details "enum")]
      [:fieldset {:class (if (> (count prop-errors) 0) "error" "") :id id}
       (concat (list [:legend (pick-title name details)])
               (if (details "description") (list [:p (details "description")]) '())
               (if (> (count prop-errors) 0) (map #(vector :p {:class "error"} %) prop-errors) '())
               (map #(vector :label
                             (let [input-attrs {:type "radio" :value % :name id}]
                               [:input (if (= (find-value values path) %)
                                         (assoc input-attrs :checked "checked")
                                         input-attrs)])
                             (s/capitalize %)) enum))])))

(defmethod render-property :default [& args]
  (println args))



