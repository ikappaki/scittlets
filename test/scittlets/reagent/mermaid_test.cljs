(ns scittlets.reagent.mermaid-test
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [clojure.string :as str]
            [scittlets.reagent.mermaid :refer [mermaid+]]))

(println :doc (meta #'mermaid+))

(def dependencies
  ["<script src=\"https://cdn.jsdelivr.net/npm/react@18/umd/react.production.min.js\"></script>"
    "<script src=\"https://cdn.jsdelivr.net/npm/react-dom@18/umd/react-dom.production.min.js\"></script>"
    "<script src=\"https://cdn.jsdelivr.net/npm/scittle@latest/dist/scittle.reagent.min.js\" type=\"application/javascript\"></script>"
   "<script src=\"https://cdn.jsdelivr.net/npm/mermaid@11.6.0/dist/mermaid.min.js \"></script>"])

(def dependencies+ (try (requiring-resolve 'scittlets.reagent.test-utils/dependencies+)
                        (catch :default error #(r/as-element [:div (str %1)]))))

(defn text-input [input*]
  [:div
   [:textarea {:value @input*
               :rows 5
               :on-change #(reset! input* (-> % .-target .-value))
               :style {:width "100%"}}]])


(defn my-component [graph*]
  [:div
   [mermaid+ @graph*]])

(comment
  "graph LR
A[💻] --> B[🔄] --> C[🧪]
A --> D[🔧] --> E[📦]
D --> F[⚡]"

  "graph LR
A[] --> B[] --> C[]
A --> D[] --> E[]
D --> F[]"  

  )

;; %%{init: {'theme': 'base', 'themeVariables': {'fontSize': '30px'}}}%%
(let [input* (r/atom "%%{init: {'themeVariables': {'fontSize': '30px'}}}%%

graph LR
    A[🌕] --> B[🍒] --> C[👻] --> D[💀]
    B --> E[✨] --> F[💨👻] --> G[🏁]";; "graph LR
;; Earth[🌍] --> Water[💧] --> Growth[🌱] --> Life[🐾]
;; Life --> Energy[💫]
;; Energy --> Earth"
              ;; "graph LR
;; A[⌛] --> B[⚙️] --> C[🧬] --> D[💫]
;; D --> A[🔄]"
;;               "graph LR
;; cljs[💻] --> repl[🔄] --> test[🧪]
;; cljs --> compile[🔧] --> build[📦]
;; compile --> optimize[⚡]"
              )]
  (rdom/render [:<>
                [dependencies+ dependencies]
                [text-input input*]
                [my-component input*]] (.getElementById js/document "app")))
