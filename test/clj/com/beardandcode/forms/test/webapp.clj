(ns com.beardandcode.forms.test.webapp
  (:require [clojure.java.shell :as shell]
            [ring.adapter.jetty :refer [run-jetty]]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [hiccup.page :as hiccup]
            [com.beardandcode.forms :refer [defschema build]]))


(defschema register-schema "schema/test.json")


(defn route-fn []
  (-> (routes

       (GET "/" [] (hiccup/html5
                    [:head
                     [:title "Test form"]
                     [:link {:rel "stylesheet" :type "text/css" :href "/static/main.css"}]]
                    [:body (build "/" register-schema)]))

       (route/resources "/static/"))))


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

