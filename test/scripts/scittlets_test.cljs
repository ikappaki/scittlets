(ns scripts.scittlets-test
  (:require ["child_process" :refer [exec]]
            ["fs" :as fs]
            ["os" :as os]
            ["path" :as path]
            [cljs.test :refer [deftest is async run-tests use-fixtures]]
            [clojure.string :as str]))

(def scittlets-cmd "npx cherry run scripts/scittlets.cljs")

(def corpus {:catalog-main "test/corpus/catalog-main.json"
             :markers "test/corpus/markers.html"
             :no-deps "test/corpus/no-deps.html"})

#_(defn compile []
  ;; Setup code before tests
  (let [path "scripts/scittlets.mjs"]
    (when (fs/existsSync path)
      (fs/unlinkSync path)))
  (println ":compiling")
  (is (exec "npx cherry compile scripts/scittlets.cljs")))

(def temp-dir-base* (atom nil))

(defn transient-dir-make! []
  (fs/mkdtempSync (path/join @temp-dir-base* "test-")))

(use-fixtures :once
  {:before #(let [base (fs/mkdtempSync (path/join (os/tmpdir) "scittlets-"))]
              ;;(println :base base)
              (reset! temp-dir-base* base))
   :after #(when-let [base @temp-dir-base*]
             ;;(println :cleaning base)
             (fs/rmSync base #js {"recursive" true "force" true}))})

(deftest test-cmd-tags
  (async done
         (exec (str scittlets-cmd " tags")
               (fn [error stdout stderr]
                 (is (nil? error))
                 (is (empty? stderr))
                 (let [tags (-> (re-find #"(?s)Release tags:\s+(.*)" stdout)
                                second
                                (str/split #"\s+"))]
                   (is (some #{"v0.1.0"} tags))
                   (is (every? #(or (str/starts-with? % "v")
                                    (some #{%} ["latest"])) tags)))
                 (done)))))

(deftest test-cmd-catalog
  (async done
         (exec (str scittlets-cmd " catalog")
               (fn [error stdout stderr]
                 (is (nil? error))
                 (is (empty? stderr))
                 (let [tag (-> (re-find #"Catalog tag    : (.*?) \(latest\)" stdout)
                               second)]
                   (is (str/starts-with? tag "v")))
                 (let [lines (str/split-lines stdout)
                       idx (.indexOf lines "Catalog scittlets:")
                       scittlets (subvec (vec lines) (inc idx))]
                   (is (some #{"scittlets.reagent.mermaid"} scittlets)))
                 (done)))))

(deftest test-cmd-update-nodeps
  (async done
         (exec (str scittlets-cmd " update " (:no-deps corpus) " scittlets.reagent.mermaid")
               (fn [error stdout stderr]
                 (is (nil? error))
                 (is (empty? stderr))
                 (is (str/includes? stdout "Scittlet markers not found in HTML file for: scittlets.reagent.mermaid"))
                 (done)))))

(defn find-scittlet-info-lines [text]
  (let [lines (str/split-lines text)
        re #"(?s)<!-- Scittlet dependencies: ([^\s]+) -->\n.*?<!-- Scittlet dependencies: end -->"
        line-starts (reductions + 0 (map #(inc (count %)) lines))]
    (reduce
     (fn [m match]
       (let [full-match (first match)
             name (second match)
             start-pos (.indexOf text full-match)
             end-pos (+ start-pos (count full-match))
             start-line (count (filter #(<= % start-pos) line-starts))
             end-line (count (filter #(<= % end-pos) line-starts))]
         (assoc m name {:start-line start-line
                        :end-line end-line
                        :text (str/split-lines full-match)})))
     {}
     (re-seq re text))))

(deftest test-cmd-update-single-dep
  (async done
         (let [src (:markers corpus)
               target (path/join (transient-dir-make!) (path/basename src))]
           (fs/copyFileSync src target)
           (exec (str scittlets-cmd " update " target " scittlets.reagent.mermaid" " -t v0.1.0")
                 (fn [error _stdout stderr]
                   (is (nil? error))
                   (is (empty? stderr))
                   (let [content (fs/readFileSync target "utf8")
                         matches (find-scittlet-info-lines content)]
                     ;;(prn matches)
                     (is (= {"scittlets.reagent.core"
                             {:start-line 10, :end-line 11,
                              :text
                              ["<!-- Scittlet dependencies: scittlets.reagent.core -->"
                               "    <!-- Scittlet dependencies: end -->"]},
                             "scittlets.reagent.mermaid"
                             {:start-line 13, :end-line 20,
                              :text
                              ["<!-- Scittlet dependencies: scittlets.reagent.mermaid -->"
                               "    <meta name=\"scittlets.reagent.mermaid.version\" content=\"v0.1.0\">"
                               "    <script src=\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\"></script>"
                               "    <script src=\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\"></script>"
                               "    <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\"></script>"
                               "    <script src=\"https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js\"></script>"
                               "    <script src=\"https://cdn.jsdelivr.net/gh/ikappaki/scittlets@v0.1.0/src/scittlets/reagent/mermaid.cljs\" type=\"application/x-scittle\"></script>"
                               "    <!-- Scittlet dependencies: end -->"]}}
                            matches)))
                   (done))))))

(deftest test-cmd-update-two-deps
  (async done
         (let [html-src (:markers corpus)
               catalog-src (:catalog-main corpus)
               html-target (path/join (transient-dir-make!) (path/basename html-src))]
           (fs/copyFileSync html-src html-target)
           (exec (str scittlets-cmd " update " html-target " -t " catalog-src)
                 (fn [error _stdout stderr]
                   (is (nil? error))
                   (is (empty? stderr))
                   (let [content (fs/readFileSync html-target "utf8")
                         matches (find-scittlet-info-lines content)]
                     (is (= {"scittlets.reagent.core"
                             {:start-line 10, :end-line 15,
                              :text
                              ["<!-- Scittlet dependencies: scittlets.reagent.core -->"
                               "    <meta name=\"scittlets.reagent.core.version\" content=\"main\">"
                               "    <script src=\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\"></script>"
                               "    <script src=\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\"></script>"
                               "    <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\"></script>"
                               "    <!-- Scittlet dependencies: end -->"
                               ]},
                             "scittlets.reagent.mermaid"
                             {:start-line 17, :end-line 24,
                              :text ["<!-- Scittlet dependencies: scittlets.reagent.mermaid -->"
                                     "    <meta name=\"scittlets.reagent.mermaid.version\" content=\"main\">"
                                     "    <script src=\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\"></script>"
                                     "    <script src=\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\"></script>"
                                     "    <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\"></script>"
                                     "    <script src=\"https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js\"></script>"
                                     "    <script src=\"src/scittlets/reagent/mermaid.cljs\" type=\"application/x-scittle\"></script>"
                                     "    <!-- Scittlet dependencies: end -->"]}}
                            matches)))
                   (done))))))

(deftest test-cmd-catalog-rewrite
  (async done
         (let [src (:catalog-main corpus)]
           (exec (str scittlets-cmd " catalog " src " -r")
                 (fn [error stdout stderr]
                   (is (nil? error))
                   (is (empty? stderr))
                   (let [lines (-> (str/split-lines stdout)
                                   (rest))]
                     (is (.parse js/JSON (str/join "\n" lines)))
                     #_(prn lines)
                     (is (= ["{"
                             "  \"version\": \"main\","
                             "  \"scittlets.reagent.core\": {"
                             "    \"home\": \"https://github.com/ikappaki/scittlets\","
                             "    \"deps\": ["
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\\\"></script>\""
                             "    ],"
                             "    \"see\": {"
                             "      \"reagent\": \"https://reagent-project.github.io/\","
                             "      \"scittle\": \"https://github.com/babashka/scittle\""
                             "    }"
                             "  },"
                             "  \"scittlets.reagent.mermaid\": {"
                             "    \"home\": \"https://github.com/ikappaki/scittlets\","
                             "    \"deps\": ["
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/gh/ikappaki/scittlets/src/scittlets/reagent/mermaid.cljs\\\" type=\\\"application/x-scittle\\\"></script>\""
                             "    ],"
                             "    \"see\": {"
                             "      \"mermaid\": \"https://mermaid.js.org/\","
                             "      \"reagent\": \"https://reagent-project.github.io/\""
                             "    }"
                             "  },"
                             "  \"templates\": {"
                             "    \"scittle/basic\": {"
                             "      \"src\": \"examples/scittle\","
                             "      \"descr\": \"A minimal Scittle template with zero dependencies\","
                             "      \"files\": ["
                             "        {"
                             "          \"src\": \"https://cdn.jsdelivr.net/gh/ikappaki/scittlets/examples/scittle/scittle_basic.html\","
                             "          \"dest\": \"index.html\""
                             "        },"
                             "        {"
                             "          \"src\": \"https://cdn.jsdelivr.net/gh/ikappaki/scittlets/examples/scittle/scittle_basic.cljs\","
                             "          \"dest\": \"scittle_basic.cljs\""
                             "        }"
                             "      ],"
                             "      \"target\": \"scittle_basic\""
                             "    }"
                             "  }"
                             "}"]
                            lines)))
                   (done))))))

(deftest test-cmd-catalog-rewrite-tag
  (async done
         (let [src (:catalog-main corpus)]
           (exec (str scittlets-cmd " catalog " src " -r v99.99.99")
                 (fn [error stdout stderr]
                   (is (nil? error))
                   (is (empty? stderr))
                   (let [lines (-> (str/split-lines stdout)
                                   (rest))]
                     (is (.parse js/JSON (str/join "\n" lines)))
                     #_(prn lines)
                     (is (= ["{"
                             "  \"version\": \"v99.99.99\","
                             "  \"scittlets.reagent.core\": {"
                             "    \"home\": \"https://github.com/ikappaki/scittlets\","
                             "    \"deps\": ["
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\\\"></script>\""
                             "    ],"
                             "    \"see\": {"
                             "      \"reagent\": \"https://reagent-project.github.io/\","
                             "      \"scittle\": \"https://github.com/babashka/scittle\""
                             "    }"
                             "  },"
                             "  \"scittlets.reagent.mermaid\": {"
                             "    \"home\": \"https://github.com/ikappaki/scittlets\","
                             "    \"deps\": ["
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js\\\"></script>\","
                             "      \"<script src=\\\"https://cdn.jsdelivr.net/gh/ikappaki/scittlets@v99.99.99/src/scittlets/reagent/mermaid.cljs\\\" type=\\\"application/x-scittle\\\"></script>\""
                             "    ],"
                             "    \"see\": {"
                             "      \"mermaid\": \"https://mermaid.js.org/\","
                             "      \"reagent\": \"https://reagent-project.github.io/\""
                             "    }"
                             "  },"
                             "  \"templates\": {"
                             "    \"scittle/basic\": {"
                             "      \"src\": \"examples/scittle\","
                             "      \"descr\": \"A minimal Scittle template with zero dependencies\","
                             "      \"files\": ["
                             "        {"
                             "          \"src\": \"https://cdn.jsdelivr.net/gh/ikappaki/scittlets@v99.99.99/examples/scittle/scittle_basic.html\","
                             "          \"dest\": \"index.html\""
                             "        },"
                             "        {"
                             "          \"src\": \"https://cdn.jsdelivr.net/gh/ikappaki/scittlets@v99.99.99/examples/scittle/scittle_basic.cljs\","
                             "          \"dest\": \"scittle_basic.cljs\""
                             "        }"
                             "      ],"
                             "      \"target\": \"scittle_basic\""
                             "    }"
                             "  }"
                             "}"]
                            lines)))
                   (done))))))

(deftest test-cmd-new
  (async done
         (let [target (path/join (transient-dir-make!) "test-cmd-new")]
           (exec (str scittlets-cmd " new " target " -t ./catalog.json")
                 (fn [error stdout stderr]
                   (is (nil? error) (str "stdout: " stdout
                                         "\n\nstderr: " stderr))
                   (is (empty? stderr))
                   (is (= ["index.html" "scittle_basic.cljs"] (js->clj (fs/readdirSync target))))
                   (let [content (fs/readFileSync (path/join target "index.html") "utf8")]
                     (println)
                     ;;(prn matches)
                     (is (= ["<!DOCTYPE html>"
                             "<html>"
                             "  <head>"
                             "    <meta charset=\"UTF-8\" />"
                             "    <title>Minimal cljs scittle app</title>"
                             "    <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.min.js\" type=\"application/javascript\"></script>"
                             "    <script type=\"application/x-scittle\" src=\"scittle_basic.cljs\"></script>"
                             "  </head>"
                             "  <body>"
                             "    <div id=\"app\"></div>"
                             "  </body>"
                             "</html>"]
                            ;; Windows CI adds a blank line that breaks tests.
                            ;; Not reproducible locally, so we filter it out for now.
                            (filter #(not (str/blank? %)) (str/split-lines content))))
                     (done)))))))


(deftest test-cmd-new-other
  (async done
         (let [target (path/join (transient-dir-make!) "test-cmd-new-other")]
           (exec (str scittlets-cmd " new " target " --template reagent/mermaid -t ./catalog.json")
                 (fn [error stdout stderr]
                   (is (nil? error) (str "stdout: " stdout
                                         "\n\nstderr: " stderr))
                   (is (empty? stderr))
                   (is (= #{"index.html" "mermaid_demo.cljs"} (set (fs/readdirSync target))))

                   (done))))))

(deftest test-cmd-new-list
  (async done
         (exec (str scittlets-cmd " new " "--list -t ./catalog.json")
               (fn [error stdout stderr]
                 (is (nil? error) (str "stdout: " stdout
                                       "\n\nstderr: " stderr))
                 (is (empty? stderr))
                 (is (every? #(.includes stdout %) ["-  reagent/mermaid" "-  scittle/basic"]) stdout)

                 (done)))))

(run-tests)
