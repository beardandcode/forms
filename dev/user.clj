(ns user
  (:require [clojure.repl :refer :all]
            [clojure.test :refer [run-all-tests]]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [vinyasa.reimport :refer [reimport]]
            [vinyasa.pull :refer [pull]]))

;; because when javac runs it loads user.clj, thus having this in the
;; ns declaration causes a circular dependency on the class being compiled
(defn load-forms []
  (require '[com.beardandcode.forms :refer :all]
           '[com.beardandcode.forms.test.webapp :refer :all]))

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
