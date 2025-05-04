(ns scittlets.reagent.mermaid
  (:require
   [reagent.core :as r]))

(defonce init (.initialize js/mermaid #js {}))

(defn mermaid+
  "Hello mermaid docstring."
  [_GRAPH]
  (let [primed?* (r/atom false)
        graph-prev* (r/atom nil)
        on-viewport?* (r/atom false)
        ;;        class (str "slide-" (random-uuid))
        class* (atom nil)
        observer (js/IntersectionObserver.
                  (fn [entries]
                    (let [on-viewport? @on-viewport?*]
                      (when-not on-viewport?
                        (doseq [entry entries]
                          (println :ENTRY entry (.-isIntersecting entry))
                          (when (.-isIntersecting entry)
                            (reset! on-viewport?* true)))))))
        prime! #(let [on-viewport? @on-viewport?*
                      primed? @primed?*]
                  (println :priming on-viewport? primed?)
                  (when (and on-viewport? (not primed?))
                    (.run js/mermaid #js {"querySelector" (str "." @class*)})
                    (reset! primed?* true)))]
    (r/create-class
     {:component-did-mount
      (fn [_this]
        (println :mounted @primed?*)
        (prime!))

      :component-did-update
      (fn [_this _old]
        (println :updated @primed?*)
        (prime!))

      :component-will-unmount
      (fn []
        (println :disconnecting...)
        (.disconnect observer))

      :reagent-render
      (fn [graph]
        (when (not= graph @graph-prev*)
          (reset! graph-prev* graph)
          (reset! class* (str "scittlet-mermaid-" (random-uuid)))
          (reset! primed?* false))
        (let [primed? @primed?*
              _on-viewport? @on-viewport?* ;; trigger when visible on viewport
              ]
          ^{:key @class*} [:div {:ref (fn [el]
                                        (println :el el :class @class*)
                                        (when el
                                          (.observe observer el)))
                                 :class @class*
                                 :style {:visibility (if primed? :visible :hidden)}}
                           graph]))})))
