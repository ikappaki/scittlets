#!/usr/bin/env bb
(require '[babashka.cli :as cli]
         '[babashka.deps :as deps])
(deps/add-deps '{:deps {io.github.babashka/sci.nrepl {;;:mvn/version "0.0.2"
                                                      :git/sha "4f7f6d652a71b5bdc0c110313a4908d956e7a97d" ;; pickup sci.nrepl describe op
                                                      }}})
(require '[sci.nrepl.browser-server :as nrepl])

(def spec {:port {:default 1339 :coerce :int :validate pos-int?}
           :help {:alias :h}})

(let [opts (cli/parse-opts *command-line-args* {:spec spec})]
  (when (:help opts)
    (println "Usage: bb nrepl-proxy.clj [--port PORT]")
    (println "  --port PORT  nREPL port (default: 1339)")
    (System/exit 0))
  
  (let [nrepl-port (:port opts)
        websocket-port 1340
        nrepl-proxy (nrepl/start-nrepl-server! {:port nrepl-port})
        _ws-server (nrepl/start-websocket-server! {:port websocket-port})]
    (deref nrepl-proxy)))
