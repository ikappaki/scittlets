;; Adapted in part from
;; https://github.com/clj-commons/etaoin/blob/2d0a4feac0fbcc67157194b4c4d95fc320121640/test/etaoin/api_test.clj
(ns scittlets.ui-test-utils
  (:require [babashka.process :as p]
            [etaoin.api :as e]
            [taoensso.timbre :as timbre])
  (:import [java.net ServerSocket]
           [java.net Socket]))



(timbre/set-level! :info)

(def ^:dynamic *driver* nil)
(def ^:dynamic *test-server-port* nil)

(defonce repl-test-state* (atom {:init false
                                 :browser nil
                                 :server nil}))

(defmacro with-ci-firefox
  "Runs F with *driver* bound to a headed webdriver, headless on CI."
  [webdriver & body]
  (let [with-firefox-macro (if (= "true" (System/getenv "GITHUB_ACTIONS"))
                              'e/with-firefox-headless
                              'e/with-firefox)]
    `(~with-firefox-macro ~webdriver
       ~@body)))

(defn fixture-driver
  "Runs F with the webdriver bound to *driver*, headless on CI, headed
  otherwise."
  [f]
  (with-ci-firefox webdriver
    (binding [*driver* webdriver]
      (f))))

(defn- available-port-find []
  (with-open [sock (ServerSocket. 0)]
    (.getLocalPort sock)))

(defn test-server-url-get [path]
  (format "http://localhost:%d/%s" *test-server-port* path))

(defn test-server-get! []
  (let [port (available-port-find)
        proc (p/process {:out :inherit :err :inherit}
                        "bb test-server --port" port)
        deadline (+ (System/currentTimeMillis) 15000)
        host "localhost"]
    (loop []
      (let [resp (try
                   (with-open [_socket (Socket. host port)]
                     :ok)
                   (catch Exception _ :not-ready))]
        (when (= :not-ready resp)
          (if (< (System/currentTimeMillis) deadline)
            (do
              (println "- waiting for test-server to be ready at" host port)
              (Thread/sleep 1000)
              (recur))
            (throw (ex-info "Timed out waiting for ready test server" {}))))))
    (println "Test server ready at " host ":" port)
    {:proc proc
     :port port}))

(defn fixture-test-server [f]
  (let [{:keys [proc port]} (test-server-get!)]
    (binding [*test-server-port* port]
      (f)
      (p/destroy proc)
      @proc)))

;; ---- repl test run helpers

#_(deref repl-test-state*)

(defn repl-test-run!
  [f]
  (let [{:keys [browser server]} @repl-test-state*]
    (when-not server
      (swap! repl-test-state* assoc :server (test-server-get!)))
    (when-not browser
      (let [driver  (e/firefox)]
        (swap! repl-test-state* assoc :browser driver))))

  (let [{:keys [browser server init]} @repl-test-state*
        {:keys [port]} server]
    (binding [*driver* browser
              *test-server-port* port]
      (when-not init
        (e/go *driver* (test-server-url-get ""))
        (swap! repl-test-state* assoc :init true))
      (e/new-window *driver* :tab)
      (e/switch-window-next *driver*)
      (try
        (f)
        (catch Exception e
          (println :on-repl/error (str e)))))))
#_(repl-test-run! #(println :driver *driver* :port *test-server-port*))
