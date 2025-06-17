#!/usr/bin/env bb

(require '[babashka.cli :as cli]
         '[clojure.string :as str]
         '[cheshire.core :as json]
         '[selmer.parser :as selmer]) 

(def catalog-path "catalog.json")
(def catalog (-> catalog-path slurp (json/parse-string)))


(def cli-spec
  {:base-path {:desc "Base path for generated HTML"
               :default "/"}})

(def cli-cmds [[:install  {:cmd "npm install -g scittlets"
                           :descr "Install the scittlets CLI globally"}]
               [:usage    {:cmd "npx scittlets"
                           :descr "Show general usage and help"}]
               [:help     {:cmd "npx scittlets CMD --help"
                           :descr "Get help for a specific command (CMD)"}]
               [:catalog  {:cmd "npx scittlets catalog"
                           :descr "List all scittlets and templates in the catalog"}]

               [:new      {:cmd "npx scittlets new"
                           :descr "Create a Scittle app from a template"}]

               [:add      {:cmd "npx scittlets add"
                           :descr "Add scittlets dependencies to an HTML file"}]

               [:update   {:cmd "npx scittlets update"
                           :descr "Update scittlet dependencies in an HTML file"}]

               [:pack     {:cmd "npx scittlets pack"
                           :descr "Pack an HTML file by inlining scripts"}]

               [:releases {:cmd "npx scittlets releases"
                           :descr "List all published versions of the scittlets Catalog"}]])

(def options (cli/parse-opts *command-line-args* {:spec cli-spec}))

(when (:help options)
  (println (cli/format-opts {:spec cli-spec}))
  (System/exit 0))

(defn safe-id [s]
  (let [s (str/replace s #"[^a-zA-Z0-9-_:.]" "-")
        s (str/replace s #"^[^a-zA-Z]+" "id-")]
    s))

(defn catalog->selmer-cli
  []
  (for [[id {:keys [cmd descr]}] cli-cmds]
    {:label (name id)
     :code_text cmd
     :code_id (safe-id id)
     :description descr}))

(defn html-main-generate [selmer-path output-path]
  (let [instructions (catalog->selmer-cli)
        context {:instructions instructions}]
    (spit output-path (selmer/render (slurp selmer-path) context)))
  (println "✅ " output-path " generated."))


(defn template-demo-url [template-info]
  (let [src-dir (template-info "src")
        first-file (first (template-info "files"))
        file-name (if (map? first-file)
                    (first-file "src")
                    first-file)]
    (str src-dir "/" file-name)))

(defn catalog->selmer-templates
  [catalog]
  (let [templates (catalog "templates")]
    (for [[template info] templates]
      (let [{nm "name" :strs [descr]} info]
        {:name nm
         :descr descr
         :template template
         :id (safe-id nm)
         :demo-url (template-demo-url info)}))))

(defn html-template-generate [catalog selmer-path output-path]
  (let [templates (catalog->selmer-templates catalog)
        context {:templates templates}]
    (spit output-path (selmer/render (slurp selmer-path) context)))
  (println "✅ " output-path " generated."))

(defn safe-id [s]
  (let [s (str/replace s #"[^a-zA-Z0-9-_:.]" "-")
        s (str/replace s #"^[^a-zA-Z]+" "id-")]
    s))

(defn catalog->selmer-scittlets
  [catalog]
  (let [scittlets (filter (fn [[_ v]]
                            (and (map? v) (contains? v "deps")))
                          catalog)]
    (for [[scittlet {:strs [api descr]}] scittlets]
      {:name scittlet
       :descr descr
       :id (safe-id scittlet)
       :url api})))

(defn html-scittlets-generate [catalog selmer-path output-path]
  (let [scittlets (catalog->selmer-scittlets catalog)
        context {:scittlets scittlets}]
    (spit output-path (selmer/render (slurp selmer-path) context)))
  (println "✅ " output-path " generated."))

(html-main-generate "scripts/selmer/main.selmer.html" "index.html")
(html-scittlets-generate catalog "scripts/selmer/scittlets.selmer.html" "scittlets.html")
(html-template-generate catalog "scripts/selmer/templates.selmer.html" "templates.html")





