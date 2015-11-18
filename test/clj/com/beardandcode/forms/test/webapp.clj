(ns com.beardandcode.forms.test.webapp
  (:require [clojure.java.shell :as shell]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.util.anti-forgery :refer [anti-forgery-field]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [hiccup.page :as hiccup]
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

(defn route-fn []
  (-> (routes

       (GET "/" [] (render-form))

       (POST "/" [:as request]
         (if-let [errors (forms/errors request register-schema)]
           (render-form errors (:form-params request))
           "Form completed successfully."))

       (route/resources "/static/"))

      wrap-anti-forgery
      wrap-params
      wrap-session))


(def server nil)

(defn webapp-port [] (when server (-> server .getConnectors first .getLocalPort)))

(defn start-webapp!
  ([] (start-webapp! (Integer. (or (System/getenv "PORT") 0))))
  ([port]
     (alter-var-root (var server)
                     (fn [server] (or server (run-jetty (route-fn) {:port port :join? false}))))
     (println (str "Listening on http://localhost:" (webapp-port) "/"))))

(defn open-webapp! []
  (shell/sh "open" (str "http://localhost:" (webapp-port) "/")))

(defn stop-webapp! []
  (alter-var-root (var server) #(when % (.stop %))))

