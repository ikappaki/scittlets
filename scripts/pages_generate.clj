#!/usr/bin/env bb

(require '[clojure.string :as str]
         '[cheshire.core :as json]
         '[selmer.parser :as selmer]) 

(def catalog-path "catalog.json")
(def catalog (-> catalog-path slurp (json/parse-string)))

(defn safe-id [s]
  (let [s (str/replace s #"[^a-zA-Z0-9-_:.]" "-")
        s (str/replace s #"^[^a-zA-Z]+" "id-")]
    s))

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

(html-template-generate catalog "scripts/selmer/templates.selmer.html" "templates.html")

(defn safe-id [s]
  (let [s (str/replace s #"[^a-zA-Z0-9-_:.]" "-")
        s (str/replace s #"^[^a-zA-Z]+" "id-")]
    s))

(defn catalog->selmer-scittlets
  [catalog]
  (let [scittlets (filter (fn [[_ v]]
                            (and (map? v) (contains? v "deps")))
                          catalog)]
    (for [[scittlet info] scittlets]
      (let [url (info "api")]
        {:name scittlet
         :id (safe-id scittlet)
         :url url}))))

(defn html-scittlets-generate [catalog selmer-path output-path]
  (let [scittlets (catalog->selmer-scittlets catalog)
        context {:scittlets scittlets}]
    (spit output-path (selmer/render (slurp selmer-path) context)))
  (println "✅ " output-path " generated."))

(html-scittlets-generate catalog "scripts/selmer/scittlets.selmer.html" "scittlets.html")




