(ns scripts.scittlets-test
  (:require ["child_process" :refer [exec execSync]]
            ["fs" :as fs]
            ["os" :as os]
            ["path" :as path]
            [cljs.test :refer [deftest is async run-tests testing use-fixtures]]
            [clojure.string :as str]))

(def scittlets-cmd "npx cherry run scripts/scittlets.cljs")

(def corpus {:catalog-main "test/corpus/catalog-main.json"
             :catalog-v1 "test/corpus/catalog-v1.json"

             :markers "test/corpus/markers.html"

             :no-scittle-dep "test/corpus/no-scittle-dep.html"
             :no-deps "test/corpus/no-deps.html"
             :one-dep "test/corpus/one-dep.html"

             :to-pack-html "test/corpus/topack.html"})

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

(deftest test-cmd-releases
  (testing "with local tags.txt"
    (try
      (let [stdout (execSync (str scittlets-cmd " releases --tags releases/tags.txt"))]
        (let [tags (-> (re-find #"(?s)Available catalog releases:\s+(.*)" (str stdout))
                       second
                       (str/split #"\s+"))]
          (is (some #{"v0.1.0"} tags))
          (is (every? #(or (str/starts-with? % "v")
                           (some #{%} ["latest"])) tags)))
        )
      (catch :default e
        (is false {:status (.-status e)
                   :stdout (.-stdout e)
                   :stderr (.-stderr e)}))))

  (testing "with remote tags (no --tags option)"
    (try
      (let [stdout (execSync (str scittlets-cmd " releases"))]
        (let [tags (-> (re-find #"(?s)Available catalog releases:\s+(.*)" (str stdout))
                       second
                       (str/split #"\s+"))]
          (is (some #{"v0.1.0"} tags))
          (is (every? #(or (str/starts-with? % "v")
                           (some #{%} ["latest"])) tags)))
        )
      (catch :default e
        (is false {:status (.-status e)
                   :stdout (.-stdout e)
                   :stderr (.-stderr e)})))))

(deftest test-cmd-catalog
  (try
    (let [stdout (execSync (str scittlets-cmd " catalog"))
          stdout (str stdout)]
      (let [tag (-> (re-find #"Using Catalog: (.*?) \(latest\)" stdout)
                    second)]
        (is (str/starts-with? tag "v")))
      (let [start-idx (.indexOf (str stdout) "📦 Available scittlets:")
            after-text (subs stdout start-idx)
            scittlets (->> (re-seq #"•\s+([^\s]+)" after-text)
                           (map second))]
        (is (some #{"scittlets.reagent.mermaid"} scittlets) scittlets)))
    (catch :default e
      (is false {:status (.-status e)
                 :stdout (.-stdout e)
                 :stderr (.-stderr e)}))))

(deftest test-cmd-update-nodeps
  (try
    (let [stdout (execSync (str scittlets-cmd " update " (:no-deps corpus) " scittlets.reagent.mermaid"))]
      (is (re-find #"(?s)❌ Error: Missing scittlet markers in HTML for:\s+• scittlets\.reagent\.mermaid"
                   (str stdout))))
    (catch :default e
        (is false {:status (.-status e)
                   :stdout (.-stdout e)
                   :stderr (.-stderr e)}))))

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
  (let [src (:markers corpus)
        target-dir (transient-dir-make!)
        target (path/join target-dir (path/basename src))
        catalog-v1 (:catalog-v1 corpus)
        copy-file-target-path (path/join target-dir "mermaid.copied.file")]
    (fs/copyFileSync src target)
    (try
      (let [_stdout (execSync (str scittlets-cmd " update " target " scittlets.reagent.mermaid" " -r " catalog-v1))
            content (fs/readFileSync target "utf8")
            matches (find-scittlet-info-lines content)]
            ;;(prn matches)

        (is (fs/existsSync copy-file-target-path) copy-file-target-path)

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
      (catch :default e
        (is false {:status (.-status e)
                   :stdout (.-stdout e)
                   :stderr (.-stderr e)})))))

(deftest test-cmd-update-two-deps
  (async done
         (let [html-src (:markers corpus)
               catalog-src (:catalog-main corpus)
               html-target (path/join (transient-dir-make!) (path/basename html-src))]
           (fs/copyFileSync html-src html-target)
           (exec (str scittlets-cmd " update " html-target " -r " catalog-src)
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
  (testing "rewrite"
    (let [catalog-src (:catalog-main corpus)
          target-dir (transient-dir-make!)
          catalog-expected (str catalog-src ".test-cmd-catalog-rewrite.rewrite.expected")
          catalog-target (path/join target-dir (path/basename catalog-src))]
      (try
        (let [stdout (execSync (str scittlets-cmd " catalog -r " catalog-src " --rewrite"))
              lines (-> (str/split-lines stdout)
                        (rest))
              output (str/join "\n" lines)]
          (is (.parse js/JSON output))
          (fs/writeFileSync catalog-target output)
;;         (fs/writeFileSync catalog-expected output) ;; rebase

          (let [content (fs/readFileSync catalog-target)
                expected (fs/readFileSync catalog-expected)]
            (is (= (str/split-lines (str expected))
                   (str/split-lines (str content)))
                (diff catalog-expected catalog-target))))
        (catch :default e
          (is false {:status (.-status e)
                     :stdout (.-stdout e)
                     :stderr (.-stderr e)})))))

  (testing "rewrite tag"
    (let [catalog-src (:catalog-main corpus)
          target-dir (transient-dir-make!)
          catalog-expected (str catalog-src ".test-cmd-catalog-rewrite.tag.expected")
          catalog-target (path/join target-dir (path/basename catalog-src))]
      (try
        (let [stdout (execSync (str scittlets-cmd " catalog -r " catalog-src " --rewrite v99.99.99"))
              lines (-> (str/split-lines stdout)
                        (rest))
              output (str/join "\n" lines)]
          (is (.parse js/JSON output))
          (fs/writeFileSync catalog-target output)
;;          (fs/writeFileSync catalog-expected output) ;; rebase

          (let [content (fs/readFileSync catalog-target)
                expected (fs/readFileSync catalog-expected)]
            (is (= (str/split-lines (str expected))
                   (str/split-lines (str content)))
                (diff catalog-expected catalog-target))))
        (catch :default e
          (is false {:status (.-status e)
                     :stdout (.-stdout e)
                     :stderr (.-stderr e)}))))))

(deftest test-cmd-new
  (async done
         (let [target (path/join (transient-dir-make!) "test-cmd-new")]
           (exec (str scittlets-cmd " new scittle/basic -d " target " -r ./catalog.json")
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
           (exec (str scittlets-cmd " new reagent/mermaid -d " target " -r ./catalog.json")
                 (fn [error stdout stderr]
                   (is (nil? error) (str "stdout: " stdout
                                         "\n\nstderr: " stderr))
                   (is (empty? stderr))
                   (is (= #{"index.html" "mermaid_demo.cljs"} (set (fs/readdirSync target))))

                   (done))))))

(deftest test-cmd-new-list
  (async done
         (exec (str scittlets-cmd " new " " -r ./catalog.json")
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
                                     " -r ./catalog.json"))
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
                                    " -r ./catalog.json"))]
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
                                    " -r ./catalog.json"))]

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
        (let [catalog-path (:catalog-main corpus)
              stdout (execSync (str scittlets-cmd " add " html-target " scittlets.reagent.codemirror"
                                    " -r " catalog-path))
              content (fs/readFileSync html-target)
              ;;_ (println :stdout \n stdout)
              ;;_ (fs/writeFileSync html-expected (fs/readFileSync html-src)) ;; create 
              ;;_ (fs/writeFileSync html-expected content)                    ;; rebase
              expected (fs/readFileSync html-expected)
              copy-file-target-path (path/join target-dir "codemirror.copied.file")]

          (is (str/includes? (str stdout)
                             "Existing scittlets in file:"))
          (is (str/includes? (str stdout)
                             "• scittlets.reagent.mermaid"))

          (is (fs/existsSync copy-file-target-path) copy-file-target-path)

          (is (= (str/split-lines (str expected))
                 (str/split-lines (str content)))
              (diff html-expected html-target)))

        (catch :default e
          (is false {:status (.-status e)
                     :stdout (.-stdout e)
                     :stderr (.-stderr e)}))))

    (testing "readding the existing dependency"
      (fs/copyFileSync html-src html-target)
      (try
        (let [stdout (execSync (str scittlets-cmd " add " html-target " scittlets.reagent.mermaid"
                                    " -r ./catalog.json"))]
          (is false (str "Unexpected: " stdout)))

        (catch :default e
          (is (str/includes? (.-stdout e)
                             "Error: these scittlets dependencies are already defined in the HTML file")))))))


(deftest test-proxy
  (testing "wihtout proxy"
    (try
      (let [stdout (execSync (str scittlets-cmd " releases "))]
        (is (str/includes? stdout "v0.3.0")))
      (catch :default e
        (is false {:status (.-status e)
                   :stdout (.-stdout e)
                   :stderr (.-stderr e)}))))

  (testing "with invalid proxy"
    (try
      (let [stdout (execSync (str scittlets-cmd " releases ")
                             #js {:env (js/Object.assign
                                        #js {}
                                        (.-env js/process)
                                        #js {:HTTPS_PROXY "https://127.0.0.1:0"})})]
        (is false stdout))
      (catch :default e
        (is (or (str/includes? (str (.-stdout e)) "connect EADDRNOTAVAIL 127.0.0.1") ;; win
                (str/includes? (str (.-stdout e)) "connect ECONNREFUSED 127.0.0.1")  ;; ubuntu
                ))))))


(deftest test-win-ca
  (testing "it does not fail."
    (try
      (let [stdout (execSync (str scittlets-cmd " releases -W"))]
        (is (str/includes? stdout "Available catalog releases: latest")) stdout)
      (catch :default e
        (is false {:status (.-status e)
                   :stdout (.-stdout e)
                   :stderr (.-stderr e)})))))

(run-tests)
