(ns scittlets.reagent.codemirror
  (:require
   [goog.object :as gobj]
   [reagent.core :as r]))


(defmacro esm-import
  "Asynchronously loads an ESM module from the given URL and imports the
  specified SYMBOLS.

  Returns a Reagent atom initially set to nil, which is reset to a map
  of keywordized symbol names to their corresponding module exports
  once loading completes."
  [url & symbols]
  (let [symbols (into [] (map str symbols))]
    `(let [imports*# (r/atom nil)]
       (-> (js/import ~url)
           (.then (fn [module#]
                    (reset! imports*# (loop [imports# {}
                                             symbols# ~symbols]
                                         ;; (println :ximports imports# symbols#)
                                        (if-let [sym# (first symbols#)]
                                          (recur (assoc imports# (keyword sym#) (gobj/get module# (str sym#)))
                                                 (rest symbols#))
                                          (assoc imports# :all module#))))))
           (.catch (fn [err#]
                     (println :esm-import/error err#))))
       imports*#)))

(def esm-codemirror*
  "A Reagent atom holding the following `codemirror` module symbols
  asynchronously loaded via `esm-import`:

  - basicSetup"
  (esm-import "https://esm.sh/codemirror" basicSetup))

(def esm-codemirror-view*
  "A Reagent atom holding the following `@codemirror/view` module
  symbols asynchronously loaded via `esm-import`:

  - EditorView"
  (esm-import "https://esm.sh/@codemirror/view" EditorView))

(defn when-esm-modules-ready+
  "Reagent component to wait for the MODULES reagent atom created with
  `esm-import` to be loaded before rendering the CHILDREN reagent
  components."
  [modules & children]
  (let [derefd (map deref modules)]
    (when (every? some? derefd)
      (into [:<>] children))))

(defn EditorView+
  "Reagent component to create a @codemirror/view EditorView instance
  using the EDITOR-VIEW-CONFIG provided."
  [EDITOR-VIEW-CONFIG]
  (let [{:keys [basicSetup] :as esm-codemirror} @esm-codemirror*
        {:keys [EditorView] :as esm-codemirror-view} @esm-codemirror-view*
        el* (atom nil)
        _editor-view* (atom nil)]
    (if-not (and esm-codemirror esm-codemirror-view)
      [:div ":CodeMirror loading..."]

      (r/create-class
       {:component-did-mount
        (fn [_this]
          (let [el @el*
                view-config (merge {:parent el
                                    :extensions [basicSetup]} EDITOR-VIEW-CONFIG)]
            (reset! _editor-view* (EditorView. (clj->js view-config)))))

        :component-will-unmount
        (fn []
          (println :CM-disconnecting...))

        :reagent-render
        (fn [_editor-view-config]
          [:div {:ref (fn [el]
                        (when el
                          (reset! el* el)))}])}))))
