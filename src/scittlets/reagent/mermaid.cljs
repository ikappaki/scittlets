(ns scittlets.reagent.mermaid
  (:require
   [reagent.core :as r]))

(defonce init (.initialize js/mermaid #js {}))

(defn mermaid+
  "Reagent component that renders a diagram in-place from the Mermaid
  DIAGRAM definition."
  [DIAGRAM]
  (let [primed?* (r/atom false)
        diagram-prev* (r/atom nil)
        on-viewport?* (r/atom false)
        class* (atom nil)
        observer (js/IntersectionObserver.
                  (fn [entries]
                    (let [on-viewport? @on-viewport?*]
                      (when-not on-viewport?
                        (doseq [entry entries]
                          (when (.-isIntersecting entry)
                            (reset! on-viewport?* true)))))))
        prime! #(let [on-viewport? @on-viewport?*
                      primed? @primed?*]
                  (when (and on-viewport? (not primed?))
                    (.run js/mermaid #js {"querySelector" (str "." @class*)})
                    (reset! primed?* true)))]
    (r/create-class
     {:component-did-mount
      (fn [_this]
        (prime!))

      :component-did-update
      (fn [_this _old]
        (prime!))

      :component-will-unmount
      (fn []
        (println :disconnecting...)
        (.disconnect observer))

      :reagent-render
      (fn [diagram]
        (when (not= diagram @diagram-prev*)
          (reset! diagram-prev* diagram)
          (reset! class* (str "scittlet-mermaid-" (random-uuid)))
          (reset! primed?* false))
        (let [primed? @primed?*
              _on-viewport? @on-viewport?* ;; trigger when visible on viewport
              ]
          ^{:key @class*} [:div {:ref (fn [el]
                                        (when el
                                          (.observe observer el)))
                                 :class @class*
                                 :style {:visibility (if primed? :visible :hidden)}}
                           diagram]))})))
