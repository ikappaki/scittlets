(ns scripts.html-local-deps-update
  "Update dependencies of all HTML files in DIRS from local catalog."
  (:require ["fs" :as fs]
            ["path" :as path]
            ["child_process" :refer [execSync]]))

(def usage "Usage: npm run scripts/html_local_deps_update.cljs catalog-path dirs..")

(def catalog-path (first *command-line-args*))
(prn :html-local-deps-update/catalog catalog-path)
(assert catalog-path usage)
(fs/accessSync catalog-path (.-R_OK fs/constants))

(def dirs (rest *command-line-args*))
(prn :html-local-deps-update/dirs dirs)
(assert (seq dirs) usage)
(doseq [dir dirs]
  (.isDirectory (fs/statSync dir)))

(defn find-html [dir]
  (let [entries (.readdirSync fs dir #js {:withFileTypes true})]
    (apply concat
           (for [e entries]
             (let [nm (.-name e)
                   p (path/join dir nm)]
               (if (.isDirectory e)
                 (find-html p)
                 (when (= ".html" (path/extname nm))
                   [p])))))))

(defn exec-cmd [file]
  (let [cmd (str "npm run scittlets -- update" " \"" file "\" -r " catalog-path)]
    (println)
    (prn :html-local-deps-update/info :file file :cmd cmd)
    (println)
    (let [{:keys [result error]} (try
                                   {:result (.toString (execSync cmd))}
                                   (catch :default e
                                     {:error e}))]
      (if result
        (do (println result)
            (prn :html-local-deps-update/done file))

        (do (println :html-local-deps-update/error "Can't process file, exiting ... ")
            (println)
            (println :html-local-deps-update/stdout)
            (when-let [stdout (.-stdout error)]
              (println (.toString stdout)))
            (println :html-local-deps-update/stderr)
            (when-let [stderr (.-stderr error)]
              (println (.toString stderr)))
            (js/process.exit 1))))))

(let [files (mapcat find-html dirs)]
  (prn :html-local-deps-update/files files)
  (doseq [file files]
    (exec-cmd file)))
