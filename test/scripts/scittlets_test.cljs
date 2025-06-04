(ns scripts.scittlets-test
  (:require ["child_process" :refer [exec execSync]]
            ["fs" :as fs]
            ["os" :as os]
            ["path" :as path]
            [cljs.test :refer [deftest is async run-tests testing use-fixtures]]
            [clojure.string :as str]))

(def scittlets-cmd "npx cherry run scripts/scittlets.cljs")

(def corpus {:catalog-main "test/corpus/catalog-main.json"
             :markers "test/corpus/markers.html"

             :no-scittle-dep "test/corpus/no-scittle-dep.html"
             :no-deps "test/corpus/no-deps.html"
             :one-dep "test/corpus/one-dep.html"

             :to-pack-html "test/corpus/topack.html"})

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

(defn diff [file1 file2]
  (try
    (execSync (str "diff " file1 " " file2))
    "No diffs."
    (catch :default e
      (str "\nDiffs between " file1 " and " file2 ":"
           "\n--stderr--\n"
           (.-stderr e)
           "\n--stdout--\n\n"
           (.-stdout e)))))

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

(deftest test-cmd-pack
  (async done
         (let [target-dir (transient-dir-make!)
               out-path (path/join target-dir "out.html")
               to-pack-html (:to-pack-html  corpus)]
           (exec (str scittlets-cmd " pack " to-pack-html  " " out-path)
                 (fn [error stdout stderr]
                   (is (nil? error) (str "stdout: " stdout
                                         "\n\nstderr: " stderr))
                   (is (empty? stderr))
                   (is (str/includes? stdout "Inlined 2 <script> elements"))
                   (let [content (fs/readFileSync out-path)]
                     ;;(prn content)
                     (is (= ["<!DOCTYPE html>"
                             "<html lang=\"en-us\">"
                             "  <head>"
                             "    <title>Mermaid Demo</title>"
                             "    <meta charset=\"utf-8\">"
                             ""
                             "    <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.min.js\" type=\"application/javascript\"></script>"
                             ""
                             "    <!-- Scittlet dependencies: scittlets.reagent.mermaid -->"
                             "    <meta name=\"scittlets.reagent.mermaid.version\" content=\"v0.3.0\">"
                             "    <script src=\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\"></script>"
                             "    <script src=\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\"></script>"
                             "    <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\"></script>"
                             "    <script src=\"https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js\"></script>"
                             "    <script type=\"application/x-scittle\" scittlets-pack-src=\"https://cdn.jsdelivr.net/gh/ikappaki/scittlets@v0.3.0/src/scittlets/reagent/mermaid.cljs\">"
                             "      (ns scittlets.reagent.mermaid"
                             "        (:require"
                             "         [reagent.core :as r]))"
                             "      "
                             "      (defonce init (.initialize js/mermaid #js {}))"
                             "      "
                             "      (defn mermaid+"
                             "        \"Reagent component that renders a diagram in-place from the Mermaid"
                             "        DIAGRAM definition.\""
                             "        [DIAGRAM]"
                             "        (let [primed?* (r/atom false)"
                             "              diagram-prev* (r/atom nil)"
                             "              on-viewport?* (r/atom false)"
                             "              class* (atom nil)"
                             "              observer (js/IntersectionObserver."
                             "                        (fn [entries]"
                             "                          (let [on-viewport? @on-viewport?*]"
                             "                            (when-not on-viewport?"
                             "                              (doseq [entry entries]"
                             "                                (when (.-isIntersecting entry)"
                             "                                  (reset! on-viewport?* true)))))))"
                             "              prime! #(let [on-viewport? @on-viewport?*"
                             "                            primed? @primed?*]"
                             "                        (when (and on-viewport? (not primed?))"
                             "                          (.run js/mermaid #js {\"querySelector\" (str \".\" @class*)})"
                             "                          (reset! primed?* true)))]"
                             "          (r/create-class"
                             "           {:component-did-mount"
                             "            (fn [_this]"
                             "              (prime!))"
                             "      "
                             "            :component-did-update"
                             "            (fn [_this _old]"
                             "              (prime!))"
                             "      "
                             "            :component-will-unmount"
                             "            (fn []"
                             "              (println :disconnecting...)"
                             "              (.disconnect observer))"
                             "      "
                             "            :reagent-render"
                             "            (fn [diagram]"
                             "              (when (not= diagram @diagram-prev*)"
                             "                (reset! diagram-prev* diagram)"
                             "                (reset! class* (str \"scittlet-mermaid-\" (random-uuid)))"
                             "                (reset! primed?* false))"
                             "              (let [primed? @primed?*"
                             "                    _on-viewport? @on-viewport?* ;; trigger when visible on viewport"
                             "                    ]"
                             "                ^{:key @class*} [:div {:ref (fn [el]"
                             "                                              (when el"
                             "                                                (.observe observer el)))"
                             "                                       :class @class*"
                             "                                       :style {:visibility (if primed? :visible :hidden)}}"
                             "                                 diagram]))})))"
                             "    </script>"
                             "    <!-- Scittlet dependencies: end -->"
                             ""
                             "    <!-- Scittle App -->"
                             "    <script type=\"application/x-scittle\" deref scittlets-pack-src=\"topack.cljs\">"
                             "      (require '[reagent.dom :as rdom]"
                             "               '[scittlets.reagent.mermaid :refer [mermaid+]])"
                             "      "
                             "      (rdom/render"
                             "       [mermaid+ \"journey"
                             "          title My working day"
                             "          section Go to work"
                             "            Make tea: 5: Me"
                             "            Go upstairs: 3: Me"
                             "            Do work: 1: Me, Cat"
                             "          section Go home"
                             "            Go downstairs: 5: Me"
                             "            Sit down: 5: Me\"]"
                             "      "
                             "       (.getElementById js/document \"app\"))"
                             "    </script>"
                             ""
                             "  </head>"
                             "  <body>"
                             "    <div id=\"app\"></div></div>"
                             "  </body>"
                             "</html>"]
                            (str/split-lines content))))
                   (done))))))

(deftest test-cmd-add
  (testing "no existing dependencies"
    (let [target-dir (transient-dir-make!)
          html-src (:no-deps corpus)
          html-expected (str html-src ".test-cmd-add.expected")
          html-target (path/join target-dir (path/basename html-src))]

      (fs/copyFileSync html-src html-target)
      (try
        (let [_stdout (execSync (str scittlets-cmd " add " html-target
                                     " scittlets.reagent.codemirror scittlets.reagent.mermaid"
                                     " -t ./catalog.json"))
              content (fs/readFileSync html-target)
              ;;_ (fs/writeFileSync html-expected (fs/readFileSync html-src)) ;; create 
              ;;_ (fs/writeFileSync html-expected content)                    ;; rebase
              expected (fs/readFileSync html-expected)]

          (is (= (str/split-lines (str expected))
                 (str/split-lines (str content)))
              (diff html-target html-expected)))

        (catch :default e
          (is false {:status (.-status e)
                     :stdout (.-stdout e)
                     :stderr (.-stderr e)})))))

  (testing "no main scittle dependency"
    (let [target-dir (transient-dir-make!)
          html-src (:no-scittle-dep corpus)
          html-target (path/join target-dir (path/basename html-src))]

      (fs/copyFileSync html-src html-target)
      (try
        (let [stdout (execSync (str scittlets-cmd " add " html-target
                                    " scittlets.reagent.codemirror"
                                    " -t ./catalog.json"))]
          (is false (str "Unexpected: " stdout)))

        (catch :default e
          (is (str/includes? (str (.-stdout e)) "Error: Missing required main Scittle <script> tag in the target HTML")))))))

(deftest test-cmd-add-to-one-dep
  (let [target-dir (transient-dir-make!)
        html-src (:one-dep  corpus)
        html-expected (str html-src ".test-add-to-one-dep.expected")
        html-target (path/join target-dir (path/basename html-src))]

    (testing "with not passing a scittlet"
      (fs/copyFileSync html-src html-target)
      (try
        (let [stdout (execSync (str scittlets-cmd " add " html-target
                                    " -t ./catalog.json"))]

          (is (str/includes? (str stdout)
                             "Scittlet dependencies found in the target HTML file"))
          (is (str/includes? (str stdout)
                             "• scittlets.reagent.mermaid")))
        (catch :default e
          (is false {:status (.-status e)
                     :stdout (.-stdout e)
                     :stderr (.-stderr e)}))))

    (testing "adding with one existing dependency"
      (fs/copyFileSync html-src html-target)
      (try
        (let [_stdout (execSync (str scittlets-cmd " add " html-target " scittlets.reagent.codemirror"
                                     " -t ./catalog.json"))
              content (fs/readFileSync html-target)

              ;;_ (fs/writeFileSync html-expected (fs/readFileSync html-src)) ;; create 
              ;;_ (fs/writeFileSync html-expected content)                    ;; rebase
              expected (fs/readFileSync html-expected)]

          (is (= (str/split-lines (str expected))
                 (str/split-lines (str content)))
              (diff html-target html-expected)))

        (catch :default e
          (is false {:status (.-status e)
                     :stdout (.-stdout e)
                     :stderr (.-stderr e)}))))

    (testing "readding the existing dependency"
      (fs/copyFileSync html-src html-target)
      (try
        (let [stdout (execSync (str scittlets-cmd " add " html-target " scittlets.reagent.mermaid"
                                    " -t ./catalog.json"))]
          (is false (str "Unexpected: " stdout)))

        (catch :default e
          (is (str/includes? (.-stdout e)
                             "Error: these scittlets dependencies are already defined in the HTML file")))))))


(deftest test-proxy
  (testing "wihtout proxy"
    (try
      (let [stdout (execSync (str scittlets-cmd " tags "))]
        (is (str/includes? stdout "v0.3.0")))
      (catch :default e
        (is false {:status (.-status e)
                   :stdout (.-stdout e)
                   :stderr (.-stderr e)}))))

  (testing "with invalid proxy"
    (try
      (let [stdout (execSync (str scittlets-cmd " tags ")
                             #js {:env (js/Object.assign
                                        (.-env js/process)
                                        #js {:HTTPS_PROXY "https://127.0.0.1:0"})})]
        (is false stdout))
      (catch :default e
        (is (str/includes? (str (.-stdout e)) "connect EADDRNOTAVAIL 127.0.0.1"))))))


(run-tests)
