(ns com.beardandcode.forms.test.webapp
  (:require [com.stuartsierra.component :as component]
            [clojure.java.shell :as shell]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [hiccup.page :as hiccup]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [com.beardandcode.components.routes :refer [new-routes]]
            [com.beardandcode.components.web-server :refer [new-web-server]]
            [com.beardandcode.forms :as forms]))


(forms/defschema register-schema "schema/test.json")
(forms/defschema nested-schema "schema/nested.json")


(defn wrap-println [handler & _]
  (fn [req]
    (println req)
    (handler req)))

(defn render-schema
  ([schema path] (render-schema schema path {} {}))
  ([schema path errors values]
   (hiccup/html5
     [:head
      [:title "Test form"]
      [:link {:rel "stylesheet" :type "text/css" :href "/static/main.css"}]]
     [:body (forms/build path schema {:errors errors
                                      :values values
                                      :csrf-fn anti-forgery-field})])))

(defn mount-schema [base-path schema]
  (ANY base-path [:as request]
       (case (:request-method request)
         :get (render-schema schema base-path)
         :post (if-let [errors (forms/errors request schema)]
                 (render-schema schema base-path errors
                                (forms/values request schema))
                 (str (forms/values request schema))))))

(defn route-fn [& _]
  (-> (routes

       (GET "/" [] (hiccup/html5
                    [:ul
                     [:li [:a {:href "/register"} "Registration schema"]]
                     [:li [:a {:href "/nested"} "A nested schema"]]]))

       (mount-schema "/register" register-schema)
       (mount-schema "/nested" nested-schema)

       (route/resources "/static/"))

      wrap-anti-forgery
      wrap-params
      wrap-session))


(defn new-test-system [port]
  (component/system-map
   :routes (new-routes route-fn)
   :web (component/using
         (new-web-server "127.0.0.1" port)
                  [:routes])))
