(ns scittlets
  (:require ["fs" :as fs]
            ["https-proxy-agent" :refer [HttpsProxyAgent]]
            ["node-fetch$default" :as fetch]
            ["path" :as path]
            ["proxy-from-env" :refer [getProxyForUrl]]
            ["yargs$default" :as yargs]
            [clojure.string :as str]))


(def v? (atom false))
(defn debug [& args]
  (when @v? (apply println args)))


(def script-filename "scittlets")

(def spec (-> (yargs)
              (.strict)
              (.wrap 120)
              (.showHelpOnFail true)
              (.scriptName script-filename)
              (.usage (str "Usage: scittlets <command> [options]"
                           "\n\nRun `scittlets <command> --help` for detailed usage of a specific command."))
              (.command "releases" "List all published versions of the scittlets Catalog.")
              (.command "catalog" "List all scittlets in the catalog."
                        (fn [y]
                          (-> y
                              (.option "release"
                                       (clj->js {:alias "r"
                                                 :description "Catalog release version to use"
                                                 :default "latest"
                                                 :type "string"}))
                              (.option "rewrite"
                                       (clj->js {:description "Rewrite source URLs to this version (may not exist) and print the result"
                                                 :type "string"})))))
              (.command "new [template]" "Create a new app from TEMPLATE. If no template is provided, lists available templates."
                        (fn [y]
                          (-> y
                               (.positional "template"
                                            (clj->js {:describe "Template name"
                                                      :type "string"}))
                               (.option "directory"
                                        (clj->js {:alias "d"
                                                  :describe "Override the default output directory for the template"
                                                  :type "string"}))
                               (.option "list-templates"
                                       (clj->js {:alias "l"
                                                 :description "List catalog templates"
                                                 :default false
                                                 :type "boolean"}))
                               (.option "release"
                                        (clj->js {:alias "r"
                                                  :description "Catalog release version to use"
                                                  :default "latest"
                                                  :type "string"})))))
              (.command "add [html] [scittlets..]" "Add SCITTLETS dependencies to the target HTML file, or list them if none are provided. Use --list-scittlets (or -l) to list available scittlets from the catalog."
                        (fn [y]
                          (-> y
                              (.positional "html"
                                           (clj->js {:describe "Path to the target HTML file to update."
                                                     :type "string"}))
                              (.positional "scittlets"
                                           (clj->js {:describe "Scittlets to add to the target HTML file"
                                                     :type "string"}))
                              (.option "list-scittlets"
                                       (clj->js {:alias "l"
                                                 :description "List catalog scittlets"
                                                 :default false
                                                 :type "boolean"}))
                              (.option "release"
                                       (clj->js {:alias "r"
                                                 :description "Catalog release version to use"
                                                 :default "latest"
                                                 :type "string"})))))
              (.command "update <html> [scittlets..]" "Update all scittlet dependencies in the HTML file at PATH from the Catalog. If SCITTLETS are specified, update only those."
                        (fn [y]
                          (-> y
                              (.positional "html"
                                           (clj->js {:describe "Path to the HTML file"
                                                     :type "string"}))
                              (.positional "scittlets"
                                           (clj->js {:describe "Limit updates to these scittlets"
                                                     :type "string"}))
                              (.option "release"
                                       (clj->js {:alias "r"
                                                 :description "Catalog release version to use"
                                                 :default "latest"
                                                 :type "string"})))))
              (.command "pack <html> [target]" "Pack HTML file by inlining script elements."
                        (fn [y]
                          (-> y
                              (.positional "path"
                                           (clj->js {:describe "Path to the source HTML file"
                                                     :type "string"}))
                              (.positional "target"
                                           (clj->js {:describe "Output HTML filename"
                                                     :default "packed.html"
                                                     :type "string"})))))
              (.option "verbose" (clj->js {:alias "v",
                                           :type "boolean"
                                           :description "Enable verbose logging"
                                           :global true
                                           :default false}))
              (.option "sec-win-ca" (clj->js {:alias "W",
                                              :type "boolean"
                                              :description "Load Windows system root certificates"
                                              :global true
                                              :default false}))
              (.epilog (str "[1] RELEASE may also be a local path to a catalog file. The special value `latest` resolves to the most recent release version."
                            "\n[2] To avoid GitHub API rate limits, set the GITHUB_PUBLIC_TOKEN env var (no scopes needed)."
                            "\n[3] Set the HTTP_PROXY, HTTPS_PROXY, or NO_PROXY environment variables to use a proxy."
                            "\n[4] Use NODE_EXTRA_CA_CERTS env variable to add custom CA certificates for HTTPS."))

              (.middleware (fn [argv]
                             (when (.-verbose argv)
                               (reset! v? true))))
              (.help)))

(def releases-url "https://api.github.com/repos/ikappaki/scittlets/releases") 
(def catalog-download-url "https://github.com/ikappaki/scittlets/releases/download/")
(def gh-token (.-GITHUB_PUBLIC_TOKEN js/process.env))
(def fetch-opts (clj->js (cond-> {:headers {"User-Agent" "scittlets"}}
                           gh-token
                           (assoc :headers {"Authorization" (str "Bearer " gh-token)}))))

(def scittlets-jsdelivr-url "https://cdn.jsdelivr.net/gh/ikappaki/scittlets")

(declare tags-get)
(declare catalog-get)
(declare deps-update!)
(declare readable?)
(declare scittlets-get)
(declare catalog-rewrite)
(declare template-new)
(declare templates-print)
(declare scittlets-add!)
(declare scittlets-print)
(declare pack)
(declare exit)
(declare win-ca-load!)

(defn ^:async dispatch [argv]
  (debug :releases/url releases-url)
  (debug :catalog/url catalog-download-url)
  (debug :env/GITHUB_PUBLIC_TOKEN (if gh-token :set :not-set) "\n")
  (let [cmd (get (.-_ argv) 0)
        arg-sec-win-ca (.-secWinCa argv)]

    (when arg-sec-win-ca
      (win-ca-load!))

    (case cmd
      "releases"
      (let [tags (js/await (tags-get))
            tags (concat ["latest"] tags)]
        (println)
        (println "✴️ Running: scittlets releases")
        (println)
        (println "🏷️ Available catalog releases:" (str/join " " tags))
        (exit 0))

      "catalog"
      (let [arg-release (.-release argv)
            arg-rewrite (.-rewrite argv)]
        (when-not arg-rewrite
          (println)
          (println "✴️ Running: scittlets catalog")
          (println))
        (let [{:keys [catalog tag]} (js/await (catalog-get arg-release))
              version-inline (get catalog "version")]
          (cond
            arg-rewrite
            (let [rewrite-tag (if (str/blank? arg-rewrite) "main" arg-rewrite)
                  catalog-up (catalog-rewrite catalog rewrite-tag)
                  json (.stringify js/JSON (clj->js catalog-up) nil 2)]
              (println json)
              (exit 0))

            :else
            (do
              (println "📚 Using Catalog:" tag (if (= arg-release tag) "" (str "(" arg-release ")")))
              (println)
              (println "🏷️ Catalog inline version:" version-inline)
              (println)
              (scittlets-print catalog)
              (println)
              (templates-print catalog)
              (exit 0)))))

      "add"
      (let [arg-html (.-html argv)
            arg-list-scittlets (.-listScittlets argv)
            arg-scittlets (.-scittlets argv)
            arg-release (.-release argv)
            {:keys [catalog tag]} (js/await (catalog-get arg-release))]
        (println)
        (println "✴️ Running: scittlets add")
        (println)

        (when arg-list-scittlets
          (scittlets-print catalog)
          (exit 0))

        (if-not (readable? arg-html)
          (exit 1 "❌ Error: Can't find, or read, file:" arg-html)
          (js/await (scittlets-add! catalog tag arg-html arg-scittlets))))

      "update"
      (let [target (.-html argv)
            arg-scittlets (.-scittlets argv)
            arg-release (.-release argv)]
        (println)
        (println "✴️ Running: scittlets update")
        (println)
        (println "📄 Target HTML:" target "\n")

        (if-not (readable? target)
          (exit 1 :update/error "Can't find, or read, file:" target)

          (let [{:keys [catalog tag]} (js/await (catalog-get arg-release))
                scittlets (scittlets-get catalog)]
            (debug :catalog/scittlets (str/join " " (keys scittlets)))
            (js/await (deps-update! tag target catalog arg-scittlets {})))))

      "new"
      (let [arg-template (.-template argv)
            arg-directory (.-directory argv)
            arg-release (.-release argv)
            arg-list-templates (.-listTemplates argv)
            {:keys [catalog tag]} (js/await (catalog-get arg-release))]
        (println)
        (println "✴️ Running: scittlets new")
        (println)
        (when arg-list-templates
          (templates-print catalog)
          (exit 0))

        (println "📚 Using Catalog:" tag)
        (println)
        (debug :new/args :template arg-template :directory arg-directory :tag arg-release)

        (if-not arg-template
          (do
            (println "⚠️ Please specify a template name.")
            (println)
            (templates-print catalog))
          (js/await (template-new tag catalog arg-template arg-directory)))
        (exit 0))

      "pack"
      (let [arg-path (.-html argv)
            arg-target (.-target argv)]
        (println)
        (println "✴️ Running: scittlets pack")
        (println)

        (if-not (readable? arg-path)
          (exit 1 "❌ Error: Can't find, or read, file:" arg-path)

          (js/await (pack arg-path arg-target))))

      nil
      (.showHelp spec)

      ;; else
      (do (println "Unknown command:" cmd "\n")
          (.showHelp spec)
          (exit 1)))))

(defn ^:async win-ca-load! []
  (when (= js/process.platform "win32")
    (debug :win-ca-load!/loading)
    (let [win-ca (js/await (js/import "win-ca"))
          default (aget win-ca "default")]
      (default #js {:inject "+"}))))

(defn ^:async data-fetch [url]
  (try
    (let [proxy-url (getProxyForUrl url)
          options-up (if (empty? proxy-url)
                       fetch-opts
                       (do (debug :data-fetch/proxy-url proxy-url)
                           (js/Object.assign fetch-opts #js {:agent (HttpsProxyAgent. proxy-url)})))
          res (js/await (fetch url options-up))]
      (if-not (.-ok res)
        (let [text (js/await (.text res))]
          (println :data-fetch/error url text)
          {:error [:data-fetch/error url text]})

        (let [tp (-> res
                     .-headers
                     (.get "content-type"))]
          (debug :fetch/type url tp)
          (cond
            (.includes tp "application/json")
            {:result (js/await (.json res))}

            (.includes tp "application/octet-stream")
            {:result (js/await (.text res))}

            (.includes tp "text/plain")
            {:result (js/await (.text res))}

            :else
            {:error [:data-fetch/error :unknown-content-type tp]}))))
    (catch :default e
      (println :data-fetch/exception e)
      {:error (str e)})))

(defn exit [code & msg]
  (apply println msg)
  (js/process.exit code))

(defn ^:async tags-get []
  (let [{:keys [result error]} (js/await (data-fetch releases-url))]
    (if error
      (exit 1 :tags-get/error error)

      (let [tags (.sort result (fn [a b] (- (js/Date. (.-published_at b)) (js/Date. (.-published_at a)))))
            tags (js->clj (->> (map #(.-tag_name %) tags)
                               ;; only the catalog releases
                               (filter #(str/starts-with? % "v"))))]
        tags))))

(defn templates-print [catalog]
  (let [templates (catalog "templates")]
    (println "📦 Available app templates:")
    (println)
    (doseq [[template props] (sort-by first templates)]
      (let [{:strs [descr]} props]
        (println " - " template "\t" descr)))
    (println)
    (println "📝 Create a new app with:

   scittlets new <template-name> [output-dir]")))

(defn ^:async catalog-get [tag]
  (let [tag (if (= tag "latest")
              (first (js/await (tags-get)))

              tag)]
    (debug :catalog/tag tag)
    (if (readable? tag)
      (let [data (fs/readFileSync tag "utf8")]
        {:tag tag :catalog  (js->clj (.parse js/JSON data))})

      (let [catalog-url (str catalog-download-url tag "/catalog.json")
            {asset :result error :error} (js/await (data-fetch catalog-url))]
        (if error
          (exit 1 :catalog-get/asset-error error)

          {:tag tag :catalog (js->clj (.parse js/JSON asset))})))))

(defn vals-with-files-rewrite [templates base]
  (reduce (fn [acc [k v]]
            (if (contains? v "files")
              (let [src (v "src")]
                (->> (update v "files"
                             (fn [files]
                               (for [file files]
                                 (if (map? file)
                                   (update file "src"
                                           (fn [src-path]
                                             (str/join "/" (remove nil? [base src src-path]))))
                                   {"src" (str/join "/" (remove nil? [base src file])) "dest" file}))))
                     (assoc acc k)))
              (assoc acc k v)))
          {} templates))

(defn catalog-rewrite [catalog rewrite-tag]
  (let [base (cond-> scittlets-jsdelivr-url
                                        (not (= rewrite-tag "main"))
                                        (str "@" (js/encodeURIComponent rewrite-tag)))
        cat-up (reduce (fn [acc [k v]]
                         (if (and (map? v) (contains? v "deps"))
                           (let [deps (get v "deps")
                                 deps-up (map #(str/replace % #"\"src/scittlets" (str \" base "/src/scittlets"))
                                              deps)]
                             (assoc acc k (assoc v "deps" deps-up)))
                           (assoc acc k v)))
                       {} catalog)
        cat-up (vals-with-files-rewrite cat-up base)
        cat-up (update cat-up "templates" vals-with-files-rewrite base)
        cat-up (assoc cat-up "version" rewrite-tag)]
    cat-up))

(defn scittlets-get [catalog]
  (into {} (filter (fn [[_k v]] (and (map? v) (contains? v "deps"))) catalog)))

(defn url-valid? [s]
  (try
    (js/URL. s)
    true
    (catch :default _ false)))

(defn ^:async template-new [tag catalog template target-dir]
  (let [templates (catalog "templates")
        template-names (keys templates)]

    (debug :template/input template target-dir)

    (if-not (contains? templates template)
      (exit 1 "❌ Template not found:" template "\n📦 Available templates:" (str/join ", " template-names))

      (let [{:strs [src files target]} (templates template)
            target-dir (or target-dir target)]
        (println "🛠️ Scaffolding new app from template:" template)
        (debug :template/info src files target)
        (debug :template/target target-dir)

        (when (fs/existsSync target-dir)
          (exit 1 "❌ Error: target directory already exists:" target-dir
                "\n\n"
                "💡 Use --directory to specify a different output folder or remove the existing one."))
        (println "📁 Creating app directory:" (path/resolve target-dir))
        (fs/mkdirSync target-dir)

        (println "📄 Generating files:")
        (doseq [entry files]
          (let [{src-file "src" dest-file "dest"} (if (map? entry)
                                                    entry
                                                    {"src" entry "dest" entry})
                dest-path (path/join target-dir dest-file)]
            (debug :template-new/files :src src-file  :dest dest-path)
            (println " ↳"  dest-path)
            (let [data (if (url-valid? src-file)
                         (let [{asset :result error :error} (js/await (data-fetch src-file))]
                           (if error
                             (exit 1 :tempalte-new/asset-error :set src-file :error error)

                             asset))
                         (let [src-path (path/join src src-file)]
                           (fs/readFileSync src-path "utf-8")))
                  html? (str/ends-with? dest-file ".html")
                  data-up (if html?
                            (-> (.replace data
                                          (re-pattern (str "src=[\"']" src "/([^\"']+)[\"']"))
                                          "src=\"$1\"")
                                (.replace  #".*<base[^>]*?>.*\n?" ""
                                           ""))

                            data)]
              (fs/writeFileSync dest-path data-up "utf8")
              (when html? (js/await (deps-update! tag dest-path catalog nil {:silent? true}))))))
        (println "
✅ Your app is ready!

👉 Next steps:
1. Install Josh globally (if you haven't already):
   npm install -g cljs-josh

2. Navigate to the app directory:
   cd" target-dir "

3. Start the development server:
   npx josh

Happy hacking! 🚀")))))

(defn get-leading-whitespace [line]
  (let [match (re-find #"^[ \t]+" line)]
    (or match "")))

(defn insert-at [v idx items]
  (into (into (subvec v 0 idx) items) (subvec v idx)))

(defn scittlet-add [lines insert-pos catalog scittlet prout]
  (let [version (get catalog "version")
        deps (get-in catalog [scittlet "deps"])]

    (if-not (seq deps)
      (throw  (js/Error. (str "❌ Error: can't find dependency: " scittlet)))

      (let [lw (get-leading-whitespace (get lines insert-pos))
            deps-meta (concat [(str "<meta name=\"" scittlet ".version\" content=\"" version "\">")] deps)
            deps-up (map #(str lw %) deps-meta)
            lines-up (insert-at lines (inc insert-pos)
                                (-> (into ["" (str lw "<!-- Scittlet dependencies: " scittlet " -->")]
                                     deps-up)
                                    (conj (str lw "<!-- Scittlet dependencies: end -->"))))]
        (prout "📦 Dependencies: ")
        (prout (str/join "\n" (map #(str "  🔹 " %) deps-up)))
        lines-up))))

(defn ^:async catalog-file-content-get [file-entry src]
  (let [{src-path "src" dest-path "dest"} (if (map? file-entry)
                                            file-entry
                                            {"src" file-entry "dest" file-entry})
        ret {:src-path src-path :dest-path dest-path}]
    (if (url-valid? src-path)
      (let [{:keys [result error]} (js/await (data-fetch src-path))]
        (if error
          (assoc ret :error error)
          (assoc ret :content result)))
      (let [src-path (path/join src src-path)]
        (assoc ret :content (fs/readFileSync src-path "utf-8"))))))

(defn  scittlets-print [catalog]
  (let [scittlets (scittlets-get catalog)
        scittlet-names (keys scittlets)]
    (println "📦 Available scittlets:")
    (println (str/join "\n" (map #(str "  • " %) scittlet-names)))
    (println)
    (println "📝 Add scittlets to an HTML file with:
   scittlets add <html-file> [scittlets..]")))
(defn ^:async scittlets-add! [catalog tag html-path scittlets]
  (let [html (.toString (fs/readFileSync html-path))
        scittlets-cat (scittlets-get catalog)
        catalog-scitts (keys scittlets-cat)
        file-scitts (->> (re-seq #"<!-- Scittlet dependencies: ((?!end)[^ ]+) -->" html)
                         (map second))
        catalog-missing (remove (set catalog-scitts) scittlets)
        file-existing (filter (set file-scitts) scittlets)]
    (println "📄 Target HTML:" (path/resolve html-path))
    (println)
    (println "📚 Using Catalog:" tag)
    (cond
      (not (seq scittlets))
      (do
        (println)
        (if (seq file-scitts)
          (do
            (println "ℹ️ Scittlet dependencies found in the target HTML file:")
            (println (str/join "\n" (map #(str "  • " %) file-scitts)))
            (println))
          (println "ℹ️ No scittlet dependencies found in target HTML file.")))

      (seq catalog-missing)
      (exit 1 "❌ Error: these scittlets dependencies are missing from the catalog:\n"
            (str/join "\n" (map #(str "  • " %) catalog-missing))

            "\n📦 Available catalog entries:\n"
            (str/join "\n" (map #(str "  • " %) catalog-scitts)))

      (seq file-existing)
      (exit 1 "❌ Error: these scittlets dependencies are already defined in the HTML file:\n"
            (str/join "\n" (map #(str "  • " %) file-existing)))

      :else
      (do
        (debug :scittlets-add!/scittlets scittlets)
        (println)
        (println "🔎 Existing scittlets in file:")
        (println  (str/join "\n" (map #(str "  • " %) file-scitts)))
        (let [lines (str/split-lines html)
              lines-indexed (map-indexed vector lines)
              scittle-pos (->> lines-indexed
                               (some (fn [[i l]] (when (re-find #"scittle\.min\.js" l) i))))
              deps-last-pos (->> lines-indexed
                                 (filter (fn [[_ l]] (re-find #"<!-- Scittlet dependencies: end -->" l)))
                                 last
                                 first)
              insert-pos (or deps-last-pos scittle-pos)]
          (debug :scittle-add!/positions :scittle scittle-pos :dep-last deps-last-pos)
          (if-not insert-pos
            (do
              (println)
              (exit 1 "❌ Error: Missing required main Scittle <script> tag in the target HTML."
                    "\n  ➕ Please add the following to the <head> of your HTML file:\n\n"
                    "  <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.min.js\" type=\"application/javascript\" deref></script>"
                    "\n"))

            (do
              (debug :scittle-add!/inserting-at insert-pos)
              (loop [remaining  (reverse scittlets)
                     lines-up lines]
                (if-let [scitt (first remaining)]
                  (do (println "\n🔧 Inserting scittlet:" scitt)
                      (let [files (get-in scittlets-cat [scitt "files"])
                            lines-up (scittlet-add lines-up insert-pos catalog scitt println)]
                        (debug :scittle-add!/updating scitt)
                        (debug :scittle-add!/files files)
                        (when (seq files)
                          (println)
                          (println "📁 Copying scittlet files:")
                          (doseq [entry files]
                            (debug :scittlet-add/file entry)
                            (let [{:keys [src-path content dest-path error]} (js/await (catalog-file-content-get entry ""))
                                  dest-path (path/join (path/dirname html-path) dest-path)]
                              (println "  →"  dest-path)
                              (debug :scittlet-add/copy src-path :to dest-path)
                              (when error
                                (exit 1 "❌ Error: can't retrieve file from" src-path ":" error))
                              (when (fs/existsSync dest-path)
                                (exit 1 "❌ Error: destination file" dest-path "already exists. Please remove to continue."))
                              (fs/writeFileSync dest-path content))))
                        (recur (rest remaining)
                               lines-up)))

                  (let [updated (str/join "\n" lines-up)]
                    (fs/writeFileSync html-path updated)
                    (println)
                    (println "✅ Scittlets added:")
                    (println  (str/join "\n" (map #(str "  • " %) scittlets)))))))))))))

(defn readable? [path]
  (try
    (.accessSync fs path (.-R_OK fs/constants))
    true
    (catch :default _ false)))

(defn replace-subvector [v start end replacement]
  (vec (concat (subvec v 0 start) replacement (subvec v end))))

(defn dep-update [lines catalog scittlet prout]
  (let [version (get catalog "version")
        deps (get-in catalog [scittlet "deps"])]

    (if-not (seq deps)
      (throw  (js/Error. (str "Error: can't find dependency: " scittlet)))

      (let [start-marker (str "<!-- Scittlet dependencies: " scittlet " -->")
            end-marker "<!-- Scittlet dependencies: end -->"
            [start-i end-i] (loop [start-i nil
                                   end-i nil
                                   cnt 0
                                   lines (rest lines)
                                   line (first lines)]
                              (if-not (seq lines)
                                [start-i end-i]

                                (cond
                                  (and start-i (str/includes? line end-marker))
                                  [start-i cnt]

                                  (str/includes? line start-marker)
                                  (recur cnt end-i (inc cnt) (rest lines) (first lines))

                                  :else
                                  (recur start-i end-i (inc cnt) (rest lines) (first lines)))))]

        (let [lw (get-leading-whitespace (get lines start-i))
              deps-meta (concat [(str "<meta name=\"" scittlet ".version\" content=\"" version "\">")] deps)
              deps-up (map #(str lw %) deps-meta)
              lines-up (replace-subvector lines (inc start-i) end-i deps-up)]
          (prout "📦 Dependencies:")
          (prout (str/join "\n" (map #(str "  🔹 " %) deps-up)))
          lines-up)))))

(defn ^:async deps-update!
  [tag html-path catalog scittlets opts]
  (let [{:keys [silent?]} opts
        prout (if silent? (fn [& _]) println)
        html    (.toString (fs/readFileSync html-path))
        catalog-scitts (filter #(and (map? (catalog %)) (contains? (catalog %) "deps")) (keys catalog))
        file-scitts (->> (re-seq #"<!-- Scittlet dependencies: ((?!end)[^ ]+) -->" html)
                         (map second))
        update-scitts (if (seq scittlets)
                        scittlets
                        file-scitts)
        catalog-missing (remove (set catalog-scitts) update-scitts)]

    (prout "📚 Using Catalog:" tag "\n")
    (if (seq catalog-missing)
      (exit 1 "❌ Error: these scittlets dependencies are missing from the catalog:\n"
            (str/join "\n" (map #(str "  • " %) catalog-missing))

            "\n\n📖 Available catalog entries:\n" (str/join "\n" (map #(str "  • " %) catalog-scitts)))

      (let [file-missing (remove (set file-scitts) update-scitts)]
        (prout "🔎 Existing scittlets in file:")
        (prout  (str/join "\n" (map #(str "  • " %) file-scitts)))

        (if (seq file-missing)
          (do (prout)
              (prout "❌ Error: Missing scittlet markers in HTML for:")
              (prout (str/join "\n" (map #(str "  • " %) file-missing)))
              (prout)
              (prout "💡 To automatically insert the missing markers, run:")
              (prout)
              (prout " npx scittlets add" html-path (str/join " " file-missing))
              (prout)
              (prout "📌 Or, manually insert the following empty markers inside the <HEAD> of the HTML file, then rerun the script:")
              (prout)
              (doseq [key file-missing]
                (let [start-marker (str "<!-- Scittlet dependencies: " key " -->")
                      end-marker "<!-- Scittlet dependencies: end -->"]
                  (prout (str " " start-marker))
                  (prout (str " " end-marker "\n"))))
              (prout "🔍 Ensure this block appears after the Scittle script tag, which typically looks like:\n"
                     "  <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.min.js\" type=\"application/javascript\" deref></script>\n"))

          (loop [remaining  update-scitts
                 lines (str/split-lines html)]
            (if-let [scitt (first remaining)]
              (do (prout)
                  (prout "♻️ Updating scittlet:" scitt)
                  (let [files (get-in catalog [scitt "files"])
                        lines-up (dep-update lines catalog scitt prout)]
                    (debug :deps-udpate!/updating scitt)
                    (debug :deps-update!/files files)

                    (when (seq files)
                      (prout)
                      (prout "📁 Copying scittlet files:")
                      (doseq [entry files]
                        (debug :deps-update/file entry)
                        (let [{:keys [src-path content dest-path error]} (js/await (catalog-file-content-get entry ""))
                              dest-path (path/join (path/dirname html-path) dest-path)]
                          (prout "  →"  dest-path)
                          (debug :scittlet-add/copy src-path :to dest-path)
                          (when error
                            (exit 1 "❌ Error: can't retrieve file from" src-path ":" error))
                          (when (fs/existsSync dest-path)
                            (let [backup-path (str dest-path "." (str (js/Math.floor (/ (.now js/Date) 1000))))]
                              (prout "    Destination file exists, renaming to:" backup-path)
                              (fs/renameSync dest-path backup-path)))
                          (fs/writeFileSync dest-path content))))

                    (recur (rest remaining)
                           lines-up)))

              (let [updated (str/join "\n" lines)]
                (fs/writeFileSync html-path updated)
                (prout)
                (prout "✅ Scittlets updated:")
                (prout  (str/join "\n" (map #(str "  • " %) update-scitts)))))))))))

(defn html-attrs-parse [tag-str]
  (let [[_ attrs-str] (re-find #"<\s*\w+\s*([^>]*)>" tag-str)
        attr-regex #"([a-zA-Z\-]+)(?:=\"([^\"]*)\")?"
        matches (re-seq attr-regex (or attrs-str ""))]
    (reduce (fn [m [_ k v]]
              (assoc m (keyword k) (if (nil? v) true v)))
            {}
            matches)))

(defn html-attrs->string [attrs-map]
  (->> attrs-map
       (map (fn [[k v]]
              (if (= v true)
                (name k)
                (str (name k) "=\"" v "\""))))
       (str/join " ")))

(defn ^:async pack [source target]
  (println "📦 Packing file:" source)
  (let [html (.readFileSync fs source "utf8")
        base (path/dirname source)
        script-regex #"([ \t]*)<script\b[^>]*>[\s\S]*?<\/script>"
        matches (re-seq script-regex html)]
    (println "🔍 Found" (count matches) "<script> elements for consideration")
    (when (seq matches)
      (loop [matches matches
             html-up html
             up-count 0]
        (if-let [[match leading-ws] (first matches)]
          (let [attrs (html-attrs-parse match)
                {:keys [src]} attrs]
            (debug :pack/info :match match :attrs attrs)
            (if-not (and src (str/ends-with? src ".cljs"))
              (recur (rest matches)
                     html-up
                     up-count)

              (do
                (println "📝 Inlining:" src)
                (let [content (if (url-valid? src)
                                (let [{:keys [result error]} (js/await (data-fetch src))]
                                  (if error
                                    (exit 1 :pack/data-fetch-error :url src :error error)
                                    result))
                                (.readFileSync fs (path/join base src) "utf8"))
                      attrs-up (-> (dissoc attrs :src)
                                   (assoc :scittlets-pack-src src))
                      script-up (str leading-ws "<script " (html-attrs->string attrs-up) ">"
                                     "\n" (->> (str/split-lines content)
                                               (map #(str leading-ws "  " %))
                                               (str/join "\n"))
                                     "\n" leading-ws "</script>")]
                  (recur (rest matches)
                         (.replace html-up match script-up)
                         (inc up-count))))))

          (let [html-up  (str/replace html-up "\r" "")]
            (println "✅ Inlined" up-count "<script> elements and saving output to" (path/resolve target))
            (fs/writeFileSync target html-up "utf8")))))))

(defn args-get [script-name]
  (let [pattern (re-pattern (str script-name "(?:\\.\\w+)?$"))
        args  (rest (drop-while #(not (re-find pattern %)) (.-argv js/process)))]
    (if (= "--" (first args))
      (rest args)
      args)))

(def args (args-get script-filename))
(def yargv (.parse spec (clj->js args)))

(js/await (dispatch yargv))
