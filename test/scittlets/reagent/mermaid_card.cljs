(ns scittlets.reagent.mermaid-card
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [scittlets.reagent.mermaid :refer [mermaid+]]
            [scittlets.reagent.test-utils :refer [info+ file-open+ demo+]]))

;; (def info+ (or (requiring-resolve 'scittlets.reagent.test-utils/info+)
;;                (fn [ns-kw] (r/as-element [:div (str ns-kw)]))))
;; (def file-open+ (or (requiring-resolve 'scittlets.reagent.test-utils/file-open+)
;;                     (fn [label url] (r/as-element [:a {:href url} (str label)]))))
;; (def demo+ (or (requiring-resolve 'scittlets.reagent.test-utils/demo+)
;;                (fn [html-url cljs-url] (r/as-element [:a {:href html-url} (str html-url)]))))

(defn text-input+ [input*]
  [:div
   [:div {:style {:margin-bottom "1rem"}}
    "- Update test: Modify text; the diagram updates."]
   [:textarea#text-input {:value @input*
                          :rows 6
                          :on-change #(reset! input* (-> % .-target .-value))
                          :style {:width "calc(100% - 10px)"
                                  :padding "2px"}}]])

(defn diagram+ [diagram*]
  [:div#diagram
   [mermaid+ @diagram*]])

(def diagram "%%{init: {'themeVariables': {'fontSize': '30px'}}}%%

graph LR
    A[🌕] --> B[🍒] --> C[👻] --> D[💀]
    B --> E[✨] --> F[💨👻] --> G[🏁]")

(let [input* (r/atom diagram)]
  (rdom/render
   [:div
    [:section.api {:style {:border-bottom "1px solid #ccc"
                           :padding-bottom "5px"}}
     [info+ :scittlets.reagent.mermaid {:vars {#'mermaid+ {:reagent? true}}}]]

    [:section.test
     [:h4 {:style {:color "#2c3e50"}} [:span "Test " [file-open+ "code" (:file (meta #'diagram+))]]]
     [text-input+ input*]
     [diagram+ input*]]

    [:section.demo
     [demo+ "examples/mermaid/mermaid_demo.html" "examples/mermaid/mermaid_demo.cljs"]]]

   (.getElementById js/document "app")))
