(ns user
  (:require [clojure.java.shell :as shell]
            [clojure.repl :refer :all]
            [clojure.test :refer [run-all-tests]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [reloaded.repl :refer [system init start stop go reset clear]]
            [vinyasa.reimport :refer [reimport]]
            [vinyasa.pull :refer [pull]]
            [com.beardandcode.components.web-server :refer [port]]))

(reloaded.repl/set-init!
 (fn []
   (require '[com.beardandcode.forms.test.webapp])
   ((ns-resolve 'com.beardandcode.forms.test.webapp 'new-test-system) (Integer. (or (System/getenv "PORT") 0)))))

(defn url [] (str "http://localhost:" (-> system :web port) "/"))
(defn open! [] (shell/sh "open" (url)))

(defn refresh-and [f]
  (refresh :after (symbol "user" f)))

(defn test-all [] (run-all-tests #"^com.beardandcode.forms.*-test$"))
(defn test-unit [] (run-all-tests #"^com.beardandcode.forms.(?!integration).*-test$"))
(defn test-integration [] (run-all-tests #"^com.beardandcode.forms.integration.*-test$"))

(defn javac []
  (reimport 'com.beardandcode.forms.SchemaWalker
            'com.beardandcode.forms.SubmitSyntaxChecker
            'com.beardandcode.forms.OrderSyntaxChecker
            'com.beardandcode.forms.PasswordFormatAttribute
            'com.beardandcode.forms.Schema)
  (refresh-all))
