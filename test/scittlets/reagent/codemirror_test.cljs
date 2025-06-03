(ns scittlets.reagent.mermaid-test
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [scittlets.reagent.codemirror :refer [EditorView+
                                                  esm-import when-esm-modules-ready+
                                                  esm-codemirror* esm-codemirror-view*]]))

(def info+ (or (requiring-resolve 'scittlets.reagent.test-utils/info+)
               (fn [ns-kw] (r/as-element [:div (str ns-kw)]))))
(def file-open+ (or (requiring-resolve 'scittlets.reagent.test-utils/file-open+)
                    (fn [label url] (r/as-element [:a {:href url} (str label)]))))
(def demo+ (or (requiring-resolve 'scittlets.reagent.test-utils/demo+)
               (fn [html-url cljs-url] (r/as-element [:a {:href html-url} (str html-url)]))))

(def esm-lang-clojure* (esm-import "https://esm.sh/@nextjournal/lang-clojure" clojure))

(defn codemirror-demo+ [TEXT SYNTAX-ERROR?*]
  (let [{:keys [basicSetup]} @esm-codemirror*
        {:keys [EditorView]} @esm-codemirror-view*
        {:keys [clojure]} @esm-lang-clojure*
        listener (-> EditorView .-updateListener
                     (.of (fn [update]
                            (when (.-docChanged update)
                              (let [txt (-> update .-state .-doc (.toString))]
                                (try
                                  (read-string txt)
                                  (reset! SYNTAX-ERROR?* false)
                                  (catch :default _
                                    (reset! SYNTAX-ERROR?* true))))))))]
    (fn [_ syntax-error?*]
      (let [syntax-error? @syntax-error?*]
        [:div
         [:div {:style {:margin-bottom "1rem"}}
          "- Listener test: Introduce a CLJS reader error (e.g., insert a stray #); the border turns red."]
         [:div {:style {:border (str "2px solid " (if syntax-error? "red" "green"))
                        :padding "8px"}}
          [EditorView+ {:doc TEXT
                        :extensions [basicSetup (clojure) listener]}]]]))))

(def example
  "(defn pacman-chomp [dots]
  (loop [remaining dots
         eaten []]
    (if (empty? remaining)
      {:eaten (conj eaten \"🍒\")  ;; bonus cherry when done!
       :left []}
      (recur (rest remaining) (conj eaten \"😋\" (first remaining))))))")

(let [syntax-error?* (r/atom false)]
  (rdom/render
   [:<>
    [:section.api {:style {:border-bottom "1px solid #ccc"
                           :padding-bottom "5px"}}
     [info+ :scittlets.reagent.codemirror {:vars {#'EditorView+ {:reagent? true}
                                                  #'esm-codemirror* {:type "reagent atom"}
                                                  #'esm-codemirror-view* {:type "reagent atom"}
                                                  #'esm-import nil
                                                  #'when-esm-modules-ready+ {:reagent? true}}}]]
    [:section.test
     [:h4 {:style {:color "#2c3e50"}} [:span "Test " [file-open+ "code" (:file (meta #'info+))]]]
     [:div
      [when-esm-modules-ready+ [esm-codemirror* esm-codemirror-view* esm-lang-clojure*]
       [codemirror-demo+ example syntax-error?*]]]]

    [:section.demo
     [demo+ "examples/codemirror/codemirror_demo.html" "examples/codemirror/codemirror_demo.cljs"]]]

   (.getElementById js/document "app")))
