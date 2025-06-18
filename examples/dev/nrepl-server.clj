#!/usr/bin/env bb
(require '[babashka.cli :as cli]
         '[babashka.deps :as deps])
(deps/add-deps '{:deps {io.github.babashka/sci.nrepl {:mvn/version "0.0.2"}}})
(require '[sci.nrepl.browser-server :as nrepl])

(def spec {:port {:default 1339 :coerce :int :validate pos-int?}
           :help {:alias :h}})

(let [opts (cli/parse-opts *command-line-args* {:spec spec})]
  (when (:help opts)
    (println "Usage: bb nrepl-server.clj [--port PORT]")
    (println "  --port PORT  nREPL port (default: 1339)")
    (System/exit 0))
  
  (let [nrepl-port (:port opts)
        websocket-port 1340
        nrepl-server (nrepl/start-nrepl-server! {:port nrepl-port})
        _ws-server (nrepl/start-websocket-server! {:port websocket-port})]
    (deref nrepl-server)))
