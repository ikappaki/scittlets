(ns scittlets.reagent.codemirror-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [etaoin.api :as e]
            [scittlets.ui-test-utils :as utu :refer [*driver*]]))

(use-fixtures :once utu/fixture-test-server)
(use-fixtures :each utu/fixture-driver)

(defn border-color-get []
  (e/get-element-css *driver* :codemirror-editor "border-color"))

(deftest border-change-test
  (doto *driver*
    (e/go (utu/test-server-url-get "api/scittlets/reagent/codemirror.html"))
    (e/refresh)
    (e/wait-visible :codemirror-editor)
    (e/click :codemirror-editor)
    (e/wait 0.1))
  (is (= "rgb(0, 128, 0)" (border-color-get)))
  (doto *driver*
    (e/perform-actions (-> (e/make-key-input)
                           (e/add-key-press "#")))
    (e/wait 0.1))
  (is (= "rgb(255, 0, 0)" (border-color-get))))

#_(utu/repl-test-run! border-change-test)
