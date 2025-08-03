(ns scripts.templates-test
  "Tests all templates in the catalog can be created and loaded with `scittlets new`."
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [cheshire.core :as json]
            [clojure.test :refer [deftest is use-fixtures]]
            [etaoin.api :as e]
            [scittlets.ui-test-utils :as utu :refer [*driver* with-ci-firefox with-test-server]]))


;; What elemetns to test has been loaded in the template.
(def  selectors {"dev/nrepl" {:xpath "//div[@id='app']//span[@id='counter' and normalize-space(text())='0']"}
                 "reagent/basic" {:css "div#app div button"}
                 "reagent/codemirror" {:css ".cm-editor"}
                 "reagent/mermaid" {:css "div#app div[class^='scittlet-mermaid'] svg[id^='mermaid']"}
                 "scittle/basic" {:css "#welcome"}})

(def catalog (json/parse-string (slurp "catalog.json")))

(defn find-scittle-srcs [html]
  (->> (re-seq #"\s*<script\s+[^>]*type=\"application/x-scittle\"[^>]*>" html)
       (map #(re-find #"src=\"([^\"]+)\"" %))
       (map second)))

(deftest scittlets-new-all-test
  ;; create an instance of each template and test some key elements
  ;; were loaded.
  (let [templates (catalog "templates")]
    (doseq [[template _]
            templates
;;            (select-keys templates ["scittle/basic"])
            ]
      (println :test/template template)
      (is (contains? templates template))
      (fs/with-temp-dir [d]
        (let [target (fs/path d "target")
              index-path (fs/path target "index.html")
              _stdout   (:out (shell (str "npm run scittlets -- new " template " " target " -r ./catalog.json")
                                    {:out :string}))]
          ;;(println :command/stdout _stdout)

          (is (fs/exists? index-path))

          (let [src-files (find-scittle-srcs (slurp (str index-path)))]
            ;; (println :index/src-files src-files)
            (doseq [src-file src-files]
              (let [dest-file-path (fs/path target src-file)]
                (if (.isAbsolute (fs/path src-file))
                  (do ;;(println :absolute-path/skipping dest-file-path)
                    )
                  (if (fs/exists? dest-file-path)
                    (do ;; (println :src-file/exists dest-file-path)
                      )
                    (do (println :src-file/copying dest-file-path)
                        (fs/create-dirs (fs/parent dest-file-path))
                        (fs/copy src-file dest-file-path)))))))
;;          (println :target/ls (:out (shell (str "ls -alR " target) {:out :string})))

          (with-test-server target
            (with-ci-firefox driver
              (let [selector (selectors template)]
                (doto driver
                  (e/go (utu/test-server-url-get "index.html"))
                  (e/refresh))
                (is (e/wait-visible driver selector {:timeout 100}))))))))))
#_(scittelts-new-all-test)



