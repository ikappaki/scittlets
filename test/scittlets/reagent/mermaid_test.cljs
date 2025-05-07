(ns scittlets.reagent.mermaid-test
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [scittlets.reagent.mermaid :refer [mermaid+]]))

(def info+ (or (requiring-resolve 'scittlets.reagent.test-utils/info+)
               (fn [ns-kw] (r/as-element [:div (str ns-kw)]))))
(def file-open+ (or (requiring-resolve 'scittlets.reagent.test-utils/file-open+)
                    (fn [label url] (r/as-element [:a {:href url} (str label)]))))

(defn text-input+ [input*]
  [:div
   [:textarea {:value @input*
               :rows 6
               :on-change #(reset! input* (-> % .-target .-value))
               :style {:width "calc(100% - 10px)"
                       :padding "2px"}}]])

(defn graph+ [graph*]
  [:div
   [mermaid+ @graph*]])

(def graph "%%{init: {'themeVariables': {'fontSize': '30px'}}}%%

graph LR
    A[🌕] --> B[🍒] --> C[👻] --> D[💀]
    B --> E[✨] --> F[💨👻] --> G[🏁]")

(let [input* (r/atom graph)]
  (rdom/render
   [:div
    [:section.api {:style {:border-bottom "1px solid #ccc"
                           :padding-bottom "5px"}}
     [info+ :scittlets.reagent.mermaid #'mermaid+]]
    [:section.demo
     [:h4 {:style {:color "#2c3e50"}} [:span "Demo " [file-open+ "code" (:file (meta #'graph+))]]]
     [text-input+ input*]
     [:div [graph+ input*]]]]

   (.getElementById js/document "app")))
