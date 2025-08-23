(ns scittlets.reagent.mermaid-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [etaoin.api :as e]
            [scittlets.ui-test-utils :as utu :refer [*driver*]]))

(use-fixtures :once utu/fixture-test-server)

(use-fixtures :each utu/fixture-driver)

(deftest text-change-test
  (doto *driver*
    (e/go (utu/test-server-url-get "api/scittlets/reagent/mermaid.html"))
    (e/refresh)
    (e/wait-visible :text-input)
    (e/click :text-input))
  (let [active-el (e/get-active-element *driver*)
        active-text (e/get-element-text-el *driver* active-el)
        subtext "🍒"
        subtext-up "🍌🍌🍌"
        text-up (str/replace active-text "🍒" subtext-up)
        xpath {:xpath (str "//span[@class='nodeLabel'][p[text()='" subtext "']]")}
        xpath-up {:xpath (str "//span[@class='nodeLabel'][p[text()='" subtext-up "']]")}]
    (is (e/wait-exists *driver* xpath))
    (is (= false (e/exists? *driver* xpath-up)))
    (e/clear *driver* :text-input)
    (e/click *driver* :text-input)
    (e/fill-active *driver* text-up)
    (is (e/wait-exists *driver* xpath-up))))
#_(utu/repl-test-run! text-change-test)
