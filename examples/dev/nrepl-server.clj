(require '[babashka.deps :as deps])

(deps/add-deps '{:deps {io.github.babashka/sci.nrepl {:mvn/version "0.0.2"}}})

(require '[sci.nrepl.browser-server :as nrepl])
(let [nrepl-port 1339
      websocket-port 1340
      nrepl-server (nrepl/start-nrepl-server! {:port nrepl-port})
      _ws-server (nrepl/start-websocket-server! {:port websocket-port})]
  (deref nrepl-server))
