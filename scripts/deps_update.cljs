(ns deps-update
  (:require ["fs" :as fs]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            nbb.core))

(def usage
  (str "Usage: npx nbb <script> [-h] [version [scittlet file]]\n"
       "  List catalog versions and scittlets, or update an HTML file with scittlet dependencies.\n\n"
       "  No arguments:                List catalog versions from GitHub releases.\n"
       "                               (Set GITHUB_PUBLIC_TOKEN to avoid API rate limits, no scopes needed)\n"
       "  <version>:                   List available scittlets in the given VERSION.\n"
       "                               (\":latest\" refers to the most recent version available)\n"
       "  <version> <scittlet>:        List metadata of the SCITTLET in VERSION.\n"
       "  <version> <scittlet> <file>: Update the HTML FILE with dependencies for the SCITTLET in VERSION.\n"))

(def args *command-line-args*)

(when (= (first args) "-h")
  (println usage)
  (js/process.exit 0))

(println :args (str/join " " args) "\n")

(def args-count (count args))
(def releases-url "https://api.github.com/repos/ikappaki/scittlets/releases")
(def gh-token (.-GITHUB_PUBLIC_TOKEN js/process.env))


(println :env/GITHUB_PUBLIC_TOKEN (if gh-token :set :not-set) "\n")
  
;;--------------------------
;; retrieve release versions


(def headers (clj->js (cond-> {:headers {"User-Agent" "nbb"}}
                        gh-token
                        (assoc "Authorization" (str "Bearer " gh-token)))))
(def releases (nbb.core/await (js/fetch releases-url headers)))
(when-not (= (.-status releases) 200)
  (println :error (js->clj releases))
  (js/process.exit 1))
(println :releases/url releases-url)
(def releases-json (nbb.core/await (.json releases)))

(def tags (js->clj (map #(.-tag_name %) releases-json)))
(assert (seq tags))
(println :realeases/versions (str/join " " tags))
(when (= args-count 0)
  (js/process.exit 0))

(def arg-tag (let [arg (first args)]
               (if (= arg ":latest")
                 (first tags)
                 arg)))

(when-not (some #{arg-tag} tags)
  (println :error "can't find version:" arg-tag)
  (js/process.exit 1))
(println)
(println :arg/version arg-tag "\n")

;;-----------------------------------
;; retrieve catalog for given version

(def catalog-url (str "https://github.com/ikappaki/scittlets/releases/download/" arg-tag "/catalog.json"))
(println :catalog/url catalog-url)
(def asset (nbb.core/await (js/fetch catalog-url headers)))
(def text (nbb.core/await (.text asset)))

;;-------------
;; parse catalog

(def catalog (js->clj (.parse js/JSON text)))
(def scittlets (into {} (filter (fn [[_k v]] (and (map? v) (contains? v "deps"))) catalog)))
(def scittlet-names (keys scittlets))
(println :catalog/scittlets (str/join " " scittlet-names))
(when (= args-count 1)
  (js/process.exit 0))
(def scittlet (second args))
(println)
(println :arg/scittlet scittlet "\n")
(when-not (some #{scittlet} scittlet-names)
  (println :error "can't find scittlet:" scittlet)
  (js/process.exit 1))

(pp/pprint {:scittlet/meta (get scittlets scittlet)})

(when (= args-count 2)
  (js/process.exit 0))

;;-------------------
;; update target file

(defn readable? [path]
  (try
    (.accessSync fs path (.-R_OK fs/constants))
    true
    (catch :default _ false)))

(def target (nth args 2))
(println :arg/file target "\n")

(when-not (readable? target)
  (println :error "Can't find, or read, file:" target)
  (js/process.exit 1))

(defn replace-subvector [v start end replacement]
  (vec (concat (subvec v 0 start) replacement (subvec v end))))

(defn get-leading-whitespace [line]
  (let [match (re-find #"^[ \t]+" line)]
    (or match "")))

(defn update-dependencies [html-path catalog key]
  (let [html    (.toString (fs/readFileSync html-path))
        version (get catalog "version")
        deps (get-in catalog [key "deps"])]
    (if-not deps
      (do (println :error "can't find dependency:" key)
          (js/process.exit 1))

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
            (pp/pprint {:deps/updating deps-up})
            (fs/writeFileSync html-path updated)
            (println :deps/updated html-path key)))))))

(update-dependencies target catalog scittlet)
