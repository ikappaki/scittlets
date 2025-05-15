(ns scittlets
  (:require ["fs" :as fs]
            ["yargs$default" :as yargs]
            [clojure.pprint :as pp]
            [clojure.string :as str]))


(def v? (atom false))
(defn debug [& args]
  (when @v? (apply println args)))


(def spec (-> (yargs)
              (.showHelpOnFail true)
              (.usage "Usage: scittlets <command> [option]")
              (.command "tags" "List all release TAGS available in the scittlet Catalog.")
              (.command "list [tag]" "List all scittlets in the catalog for the specified TAG."
                        (clj->js {:tag {:alias "t"
                                        :description "Catalog tag to use"
                                        :default "latest"
                                        :type "string"}}))
              (.command "update <path> <scittlet> [tag]" "Update existing SCITTLET dependencies in the HTML file at PATH using the catalog at the specified TAG."
                        (fn [y]
                          (-> y
                              (.positional "path"
                                           (clj->js {:describe "Path to the HTML file"
                                                     :type "string"}))
                              (.positional "scittlet"
                                           (clj->js {:describe "Scittlet to update"
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
              (.epilog "To avoid GitHub API rate limits, set the GITHUB_PUBLIC_TOKEN env var (no scopes needed).")
              (.middleware (fn [argv] (when (.-verbose argv)
                                        (reset! v? true))))
              (.help)))

(def gh-token (.-GITHUB_PUBLIC_TOKEN js/process.env))
(def releases-url "https://api.github.com/repos/ikappaki/scittlets/releases") 
(def catalog-download-url "https://github.com/ikappaki/scittlets/releases/download/")
(def headers (clj->js (cond-> {:headers {"User-Agent" "scittlets"}}
                        gh-token
                        (assoc "Authorization" (str "Bearer " gh-token)))))

(declare tags-get)
(declare catalog-get)
(declare deps-update!)
(declare readable?)
(declare exit)

(defn ^:async dispatch [argv]
  (debug :releases/url releases-url)
  (debug :catalog/url catalog-download-url)
  (debug :env/GITHUB_PUBLIC_TOKEN (if gh-token :set :not-set) "\n")


  (let [cmd (get (.-_ argv) 0)]
    (case cmd
      "tags"
      (let [tags (js/await (tags-get))]
        (println)
        (println "Release tags: " (str/join " " tags))

        (exit 0))

      "list"
      (let [arg-tag (.-tag argv)
            _ (println "Catalog tag:" arg-tag)
            catalog (js/await (catalog-get arg-tag))
            scittlets (into {} (filter (fn [[_k v]] (and (map? v) (contains? v "deps"))) catalog))
            scittlet-names (keys scittlets)]
        (println "\nCatalog Scittlets:\n" (str/join "\n" scittlet-names))

        (exit 0))

      "update"
      (let [target (.-path argv)
            scittlet (.-scittlet argv)
            tag (.-tag argv)]
        (println "\nFile to update:" target "\n")

        (if-not (readable? target)
          (exit 1 :error "Can't find, or read, file:" target)

          (let [catalog (js/await (catalog-get tag))
                scittlets (into {} (filter (fn [[_k v]] (and (map? v) (contains? v "deps"))) catalog))
                scittlet-names (keys scittlets)]
            (debug :catalog/scittlets (str/join " " scittlet-names))
            (if-not (some #{scittlet} scittlet-names)
              (exit 1 :error "can't find scittlet:" scittlet)

              (deps-update! target catalog scittlet)))))

      ;; else
      (do (when cmd (println :error ":command-unknown" cmd))
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
      (println :data-fetch-exception e)
      {:error (str e)})))

(defn exit [code & msg]
  (apply println msg)
  (js/process.exit code))

(defn ^:async tags-get []
  (let [{:keys [result error]} (js/await (data-fetch releases-url))]
    (if error
      (exit 1 :tags-get/error error)

      (let [tags (.sort result (fn [a b] (- (js/Date. (.-published_at b)) (js/Date. (.-published_at a)))))
            tags (js->clj (map #(.-tag_name %) tags))]
        tags))))

(defn ^:async catalog-get [tag]
  (let [tag (if-not (= tag "latest")
              tag

              (let [{:keys [result error]} (js/await (data-fetch (str releases-url "/latest")))]
                (if error
                  (exit 1 :catalog-get/error error)

                  (.-tag_name result))))

        _ (debug :catalog/tag tag)
        catalog-url (str catalog-download-url tag "/catalog.json")
        {asset :result error :error} (js/await (data-fetch catalog-url))]
    (if error
      (exit 1 :catalog-get/asset-error error)

      (js->clj (.parse js/JSON asset)))))

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

(defn deps-update! [html-path catalog key]
  (let [html    (.toString (fs/readFileSync html-path))
        version (get catalog "version")
        deps (get-in catalog [key "deps"])]

    (if-not deps
      (throw  (js/Error. (str "Error: can't find dependency: " key)))

      (let [start-marker (str "<!-- Scittlet dependencies: " key " -->")
            end-marker "<!-- Scittlet dependencies: end -->"
            lines (str/split-lines html)
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

        (if-not (and start-i end-i)
          (println
           "Scittlet markers not found in HTML file for:" key "\n\n"
           "Please place the following empty markers inside the <HEAD> of the HTML file, then rerun the script:\n\n"
           (str " " start-marker "\n")
           (str " " end-marker "\n\n")
           "Ensure this block appears after the Scittle script tag, which typically looks like:\n"
           "  <script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.min.js\" type=\"application/javascript\"></script>\n")

          (let [lw (get-leading-whitespace (get lines start-i))

                deps-meta (concat [(str "<meta name=\"" key ".version\" content=\"" version "\">")] deps)
                deps-up (map #(str lw %) deps-meta)
                lines-up (replace-subvector lines (inc start-i) end-i deps-up)
                updated (str/join "\n" lines-up)]
            (println "Scittlet deps:\n" (str/join "\n" deps-up))
            ;; (pp/pprint {:deps/updating deps-up})
            (fs/writeFileSync html-path updated)
            (println "\nDeps updated:" html-path key)))))))

(defn args-get [script-name]
  (let [pattern (re-pattern (str script-name "\\.\\w+$"))
        args  (rest (drop-while #(not (re-find pattern %)) (.-argv js/process)))]
    (if (= "--" (first args))
      (rest args)
      args)))

(def args (args-get "scittlets"))
(def yargv (.parse spec (clj->js args)))

(js/process.exit (js/await (dispatch yargv)))
