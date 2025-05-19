(require '[reagent.core :as r]
         '[reagent.dom :as rdom]
         '[scittlets.reagent.codemirror :refer [EditorView+
                                                esm-import when-esm-modules-ready+
                                                esm-codemirror* esm-codemirror-view*]])

g(def esm-lang-clojure*
  "A Reagent atom holding the `clojure` `@nextjournal/lang-clojure`
  module symbol asynchronously loaded via `esm-import`."
  (esm-import "https://esm.sh/@nextjournal/lang-clojure" clojure))

(def invaders
"[:h2 \"[😮 Hiccup Invaders 👾]\"
 [:div
  [:svg {:viewBox \"0 0 120 100\" :width 240 :height 200}
   ;; invaders
   [:g {:fill \"lime\"}
    [:rect {:x 20 :y 10 :width 16 :height 10}]
    [:rect {:x 50 :y 10 :width 16 :height 10}]
    [:rect {:x 80 :y 10 :width 16 :height 10}]]
   [:g {:fill \"lime\"}
    [:rect {:x 20 :y 30 :width 16 :height 10}]
    [:rect {:x 50 :y 30 :width 16 :height 10}]
    [:rect {:x 80 :y 30 :width 16 :height 10}]]
   ;; ship
   [:polygon {:points \"60,90,50,75 70,75\" :fill \"pink\"}]
   ;; laser beam
   [:line {:x1 60 :y1 65 :x2 60 :y2 55 :stroke \"red\" :stroke-width 2}]]]]")

(defn codemirror-clojure+
  "A reagent EditorView+ component that initial displays TEXT, with
  three extensions:

  - `codemirror: basicSetup`

  - `@codemirror/lang-clonjure: clojure`

  - an EditorView update listener that syncs changes to the TXT*
  atom."
  [TEXT TXT*]
  (let [{:keys [basicSetup]} @esm-codemirror*
        {:keys [EditorView]} @esm-codemirror-view*
        {:keys [clojure]} @esm-lang-clojure*
        listener (-> EditorView
                     .-updateListener
                     (.of (fn [update]
                            (when (.-docChanged update)
                              (reset! TXT* (-> update .-state .-doc (.toString)))))))]
    (fn [_ _]
      [:div {:style {:padding "8px"}}
       [EditorView+ {:doc TEXT
                     :extensions [basicSetup (clojure) listener]}]])))


(defn as-react-element+
  "A Reagent component that renders the hiccup code from the TXT* atom
  as a React element.  Returns a hiccup error message if conversion
  fails."
  [txt*]
  (let [txt @txt*]
    (try
      (r/as-element (read-string txt))
      (catch :default e
        [:pre {:style {:color "#a00" :background "#fee" :padding "6px"}} (str e)]))))

(let [txt* (r/atom invaders)
      txt @txt*]
  (rdom/render
   ;; wait for the esm modules to load befor rendering
   [when-esm-modules-ready+ [esm-codemirror* esm-codemirror-view* esm-lang-clojure*]
    [codemirror-clojure+ txt txt*]
    [as-react-element+ txt*]]

   (.getElementById js/document "app")))
