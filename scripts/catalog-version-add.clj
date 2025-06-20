(require '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[babashka.process :refer [process]])

(def tags-path "releases/tags.txt")
(def catalog-path "releases/catalog.json")

(defn usage []
  (println "Usage: bb add-version.clj <version>")
  (System/exit 1))

(let [args *command-line-args*]
  (if (not= 1 (count args))
    (usage)
    (let [version (first args)
          lines (with-open [rdr (io/reader tags-path)]
                  (doall (line-seq rdr)))]
      (when (some #(= version %) lines)
        (println "❌ Error: Version already exists in" tags-path ":" version)
        (System/exit 2))

      ;; Prepend version to tags file
      (with-open [wrt (io/writer tags-path)]
        (.write wrt (str version "\n"))
        (doseq [line lines]
          (.write wrt (str line "\n"))))
      (println (str "✅ Version " version " added to " tags-path))

      ;; Run npm catalog rewrite
      (let [cmd ["npm" "run" "scittlets" "--silent" "--" "catalog" "-r" "./catalog.json" "--rewrite" version]
            p (process cmd {:out :string :err :string})
            {:keys [out err exit]} @p
            output-lines (->> (str/split-lines out) (rest) (str/join "\n"))]
        (if (zero? exit)
          (do
            (spit catalog-path output-lines)
            (println (str "✅ Version " version " added to " catalog-path)))
          (do
            (println "❌ npm command failed:\n" err)
            (System/exit exit)))))))
