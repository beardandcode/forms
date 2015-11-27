(ns com.beardandcode.forms.test.webapp
  (:require [com.stuartsierra.component :as component]
            [clojure.java.shell :as shell]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [hiccup.page :as hiccup]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [com.beardandcode.components.routes :refer [new-routes]]
            [com.beardandcode.components.web-server :refer [new-web-server]]
            [com.beardandcode.forms :as forms]))


(forms/defschema register-schema "schema/test.json")


(defn wrap-println [handler & _]
  (fn [req]
    (println req)
    (handler req)))

(defn render-form
  ([] (render-form {} {}))
  ([errors values]
   (hiccup/html5
     [:head
      [:title "Test form"]
      [:link {:rel "stylesheet" :type "text/css" :href "/static/main.css"}]]
     [:body (forms/build "/" register-schema {:errors errors
                                              :values values
                                              :csrf-fn anti-forgery-field})])))

(defn route-fn [& _]
  (-> (routes

       (GET "/" [] (render-form))

       (POST "/" [:as request]
         (if-let [errors (forms/errors request register-schema)]
           (render-form errors (:form-params request))
           "Form completed successfully."))

       (route/resources "/static/"))

      wrap-anti-forgery
      wrap-params))


(defn new-test-system [port]
  (component/system-map
   :routes (new-routes route-fn)
   :web (component/using
         (new-web-server "127.0.0.1" port)
                  [:routes])))
