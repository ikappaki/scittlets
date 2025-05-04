(ns scittlets.reagent.mermaid-test
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [scittlets.reagent.mermaid :refer [mermaid+]]))

(def info+ (try (requiring-resolve 'scittlets.reagent.test-utils/info+)
                (catch :default error #(r/as-element [:div (str %1)]))))

(defn text-input+ [input*]
  [:div
   [:textarea {:value @input*
               :rows 5
               :on-change #(reset! input* (-> % .-target .-value))
               :style {:width "calc(100% - 10px)"}}]])


(defn graph+ [graph*]
  [:div
   [mermaid+ @graph*]])

(print :metax (meta #'text-input+)
 )
(let [input* (r/atom "%%{init: {'themeVariables': {'fontSize': '30px'}}}%%

graph LR
    A[🌕] --> B[🍒] --> C[👻] --> D[💀]
    B --> E[✨] --> F[💨👻] --> G[🏁]")]
  (rdom/render
   [:div
    [:section.api {:style {:border-bottom "1px solid #ccc"}}
     [info+ :scittlets.reagent.mermaid #'mermaid+]]
    [:section.demo
     [:h4 {:style {:color "#2c3e50"}} [:span "Demo " [:a {:href (:file (meta #'graph+)) :target "_blank"} "[🔗]"]]]
     [text-input+ input*]
     [graph+ input*]]]

   (.getElementById js/document "app")))
