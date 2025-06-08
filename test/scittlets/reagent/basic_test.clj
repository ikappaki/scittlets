(ns scittlets.reagent.basic-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [etaoin.api :as e]
            [scittlets.ui-test-utils :as utu :refer [*driver*]]))

(use-fixtures :once utu/fixture-test-server)
(use-fixtures :each utu/fixture-driver)

(defn get-text-decorations [driver el]
  (or (e/get-element-css driver el :text-decoration) ""))

(deftest todo-done-test
  (doto *driver*
    (e/go (utu/test-server-url-get  "test/scittlets/reagent/basic.html"))
    (e/refresh)
    (e/wait-visible :todo-cb-1))
  (is (= false (e/selected? *driver* :todo-cb-1)))
  (is (not (str/includes? (get-text-decorations *driver* :todo-text-1) "line-through")))
  (e/click *driver* :todo-cb-1)
  (is (= true (e/selected? *driver* :todo-cb-1)))
  (e/wait-predicate #(str/includes? (get-text-decorations *driver* :todo-text-1) "line-through")))
#_(utu/repl-test-run! todo-done-test)
