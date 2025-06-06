(ns scittlets.reagent.basic-test
  (:require [babashka.process :as p]
            [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [etaoin.api :as e])
  (:import [java.net ServerSocket]
           [java.net Socket]))

;; etaoin/test/etaoin/api_test.clj

(def ^:dynamic *driver* nil)
(def ^:dynamic *test-server-port* nil)

(defn fixture-driver
  "Executes a test running a driver. Bounds a driver
   with the global *driver* variable."
  [f]
  (e/with-firefox driver
    (binding [*driver* driver]
      (f))
    ))

(defn- find-available-port []
  (with-open [sock (ServerSocket. 0)]
    (.getLocalPort sock)))

(defn test-server-url [path]
  (format "http://localhost:%d/%s" *test-server-port* path))

(defn test-server [f]
  (binding [*test-server-port* (find-available-port)]
    (let [proc (p/process {:out :inherit :err :inherit}
                          "bb test-server --port" *test-server-port*)]
      (let [deadline (+ (System/currentTimeMillis) 15000)
            host "localhost"]
        (loop []
          (let [resp (try
                       (with-open [_socket (Socket. host  *test-server-port*)]
                         :ok)
                       (catch Exception _ :not-ready))]
            (when (= :not-ready resp)
              (if (< (System/currentTimeMillis) deadline)
                (do
                  (println "- waiting for test-server to be ready at" host *test-server-port*)
                  (Thread/sleep 1000)
                  (recur))
                (throw (ex-info "Timed out waiting for ready test server" {}))))))
        (println "Test server ready"))
      (f)
      (p/destroy proc)
      @proc)))

(use-fixtures
  :each ;; start and stop driver for each test
  test-server
  fixture-driver)

(defn get-text-decorations [driver el]
  (or (e/get-element-css driver el :text-decoration) ""))

(deftest todo-done-test
  (println :port *test-server-port*)
  (doto *driver*
    (e/go (test-server-url  "test/scittlets/reagent/basic.html")
          ;;"http://localhost:8001/test/scittlets/reagent/basic.html"
          )
    (e/refresh)
    (e/wait-visible :todo-cb-1))
  (is (= false (e/selected? *driver* :todo-cb-1)))
  (is (not (str/includes? (get-text-decorations *driver* :todo-text-1) "line-through")))
  (e/click *driver* :todo-cb-1)
  (is (= true (e/selected? *driver* :todo-cb-1)))
  (e/wait-predicate #(str/includes? (get-text-decorations *driver* :todo-text-1) "line-through")))
#_(fixture-driver todo-done-test)
