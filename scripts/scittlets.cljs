(ns scittlets
  (:require ["fs" :as fs]
            ["yargs$default" :as yargs]
            [clojure.string :as str]))


(def v? (atom false))
(defn debug [& args]
  (when @v? (apply println args)))


(def script-filename "scittlets")

(def spec (-> (yargs)
              (.wrap 120)
              (.showHelpOnFail true)
              (.scriptName script-filename)
              (.usage "Usage: scittlets <command> [options]")
              (.command "tags" "List all release TAGS of the scittlets Catalog.")
              (.command "catalog [tag] [options]" "List all scittlets in the catalog for the specified TAG."
                        (fn [y]
                          (-> y
                              (.positional "tag"
                                           (clj->js {:description "Catalog tag to use"
                                                     :default "latest"
                                                     :type "string"}))
                              (.option "rewrite"
                                       (clj->js {:alias "r"
                                                 :description "Rewrite source dependency URLs to this tag (may not exist) and print the result"
                                                 :type "string"})))))
              (.command "update <path> [scittlets..]" "Update all scittlet dependencies in the HTML file at PATH from the Catalog. If SCITTLETS are specified, update only those."
                        (fn [y]
                          (-> y
                              (.positional "path"
                                           (clj->js {:describe "Path to the HTML file"
                                                     :type "string"}))
                              (.positional "scittlets"
                                           (clj->js {:describe "Limit updates to these scittlets"
                                                     :type "string"}))
                              (.option "tag"
                                       (clj->js {:alias "t"
                                                 :description "Catalog tag to use"
                                                 :default "latest"
                                                 :type "string"})))))
              (.option "verbose" (clj->js {:alias "v",
                                           :type "boolean"
                                           :description "Enable verbose logging"
                                           :global true
                                           :default false}))
              (.epilog (str "[1] TAG may also be a local path to a catalog file. The special value `latest` resolves to the most recent release tag."
                            "\n[2] To avoid GitHub API rate limits, set the GITHUB_PUBLIC_TOKEN env var (no scopes needed)."
                            ))
              (.middleware (fn [argv] (when (.-verbose argv)
                                        (reset! v? true))))
              (.help)))

(def releases-url "https://api.github.com/repos/ikappaki/scittlets/releases") 
(def catalog-download-url "https://github.com/ikappaki/scittlets/releases/download/")
(def gh-token (.-GITHUB_PUBLIC_TOKEN js/process.env))
(def headers (clj->js (cond-> {:headers {"User-Agent" "scittlets"}}
                        gh-token
                        (assoc :headers {"Authorization" (str "Bearer " gh-token)}))))

(def scittlets-jsdelivr-url "https://cdn.jsdelivr.net/gh/ikappaki/scittlets")

(declare tags-get)
(declare catalog-get)
(declare deps-update!)
(declare readable?)
(declare scittlets-get)
(declare catalog-rewrite)
(declare exit)

(defn ^:async dispatch [argv]
  (debug :releases/url releases-url)
  (debug :catalog/url catalog-download-url)
  (debug :env/GITHUB_PUBLIC_TOKEN (if gh-token :set :not-set) "\n")

  (let [cmd (get (.-_ argv) 0)]
    (case cmd
      "tags"
      (let [tags (js/await (tags-get))
            tags (concat ["latest"] tags)]
        (println)
        (println "Release tags:" (str/join " " tags))

        (exit 0))

      "catalog"
      (let [arg-tag (.-tag argv)
            arg-rewrite (.-rewrite argv)
            {:keys [catalog tag]} (js/await (catalog-get arg-tag))
            scittlets (scittlets-get catalog)
            scittlet-names (keys scittlets)
            version (get catalog "version")]
        (cond
          arg-rewrite
          (let [rewrite-tag (if (str/blank? arg-rewrite) "main" arg-rewrite)
                catalog-up (catalog-rewrite catalog rewrite-tag)
                json (.stringify js/JSON (clj->js catalog-up) nil 2)]
            (println json)
            (exit 0))

          :else
          (do
            (println "Catalog tag    :" tag (if (= arg-tag tag) "" (str "(" arg-tag ")")))
            (println "Catalog version:" version)
            (println "\nCatalog scittlets:")
            (println (str/join "\n" scittlet-names))
            (exit 0))))

      "update"
      (let [target (.-path argv)
            arg-scittlets (.-scittlets argv)
            arg-tag (.-tag argv)]
        (println "\nFile to update:" target "\n")

        (if-not (readable? target)
          (exit 1 :update/error "Can't find, or read, file:" target)

          (let [{:keys [catalog tag]} (js/await (catalog-get arg-tag))
                scittlets (scittlets-get catalog)]
            (debug :catalog/scittlets (str/join " " (keys scittlets)))
            (deps-update! tag target catalog arg-scittlets))))

      nil
      (.showHelp spec)

      ;; else
      (do (println "Unknown command:" cmd "\n")
          (.showHelp spec)
          (exit 1)))))

(defn ^:async data-fetch [url]
  (try
    (let [res (js/await (js/fetch url headers))]
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

            :else
            {:error [:data-fetch/error :uknown-content-type tp]}))))
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

(defn catalog-rewrite [catalog rewrite-tag]
  (let [cat-up (reduce (fn [acc [k v]]
                         (if (and (map? v) (contains? v "deps"))
                           (let [base (cond-> scittlets-jsdelivr-url
                                        (not (= rewrite-tag "main"))
                                        (str "@" rewrite-tag))
                                 deps (get v "deps")
                                 deps-up (map #(str/replace % #"\"src/scittlets" (str \" base "/src/scittlets"))
                                              deps)]
                             (assoc acc k (assoc v "deps" deps-up)))
                           (assoc acc k v)))
                       {} catalog)
        cat-up (assoc cat-up "version" rewrite-tag)]
    cat-up))

(defn scittlets-get [catalog]
  (into {} (filter (fn [[_k v]] (and (map? v) (contains? v "deps"))) catalog)))

(defn readable? [path]
  (try
    (.accessSync fs path (.-R_OK fs/constants))
    true
    (catch :default _ false)))

(defn replace-subvector [v start end replacement]
  (vec (concat (subvec v 0 start) replacement (subvec v end))))

(defn get-leading-whitespace [line]
  (let [match (re-find #"^[ \t]+" line)]
    (or match "")))

(defn dep-update! [lines catalog scittlet]
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
          (println "Scittlet deps:")
          (println (str/join "\n" deps-up))
          lines-up)))))

(defn deps-update! [tag html-path catalog scittlets]
  (let [html    (.toString (fs/readFileSync html-path))
        catalog-scitts (keys catalog)
        file-scitts (->> (re-seq #"<!-- Scittlet dependencies: ((?!end)[^ ]+) -->" html)
                         (map second))
        update-scitts (if (seq scittlets)
                        scittlets
                        file-scitts)
        catalog-missing (remove (set catalog-scitts) update-scitts)]

    (println "Catalog:" tag "\n")
    (if (seq catalog-missing)
      (exit 1 "Error: these scittlets dependencies are missing from the catalog:"
            (str/join ", " catalog-missing)

            "\nAvailable catalog entries:" (str/join ", " catalog-scitts))

      (let [file-missing (remove (set file-scitts) update-scitts)]
        (println "Scittlets in file  :" (str/join ", " file-scitts))
        (println "Scittlets to update:" (str/join ", " update-scitts))
        (if (seq file-missing)
          (do (println "Scittlet markers not found in HTML file for:" (str/join ", " file-missing))
              (println)
              (println "Please place the following empty markers inside the <HEAD> of the HTML file, then rerun the script:")
              (println)
              (doseq [key file-missing]
                (let [start-marker (str "<!-- Scittlet dependencies: " key " -->")
                      end-marker "<!-- Scittlet dependencies: end -->"]
                  (println (str " " start-marker))
                  (println (str " " end-marker "\n"))))
              (println "Ensure this block appears after the scittle script tag, which typically looks like:\n"
                       "  <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.min.js\" type=\"application/javascript\" deref></script>\n"))

          (loop [remaining  update-scitts
                 lines (str/split-lines html)]
            (if-let [scitt (first remaining)]
              (do (println "\nUpdating scittlet:" scitt)
                  (let [lines-up (dep-update! lines catalog scitt)]
                    (debug :deps/updating scitt)
                    (recur (rest remaining)
                           lines-up)))

              (let [updated (str/join "\n" lines)]
                (fs/writeFileSync html-path updated)
                (println "\nScittlets updated:" (str/join ", " update-scitts))))))))))

(defn args-get [script-name]
  (let [pattern (re-pattern (str script-name "\\.\\w+$"))
        args  (rest (drop-while #(not (re-find pattern %)) (.-argv js/process)))]
    (if (= "--" (first args))
      (rest args)
      args)))

(def args (args-get script-filename))
(def yargv (.parse spec (clj->js args)))

(js/await (dispatch yargv))
