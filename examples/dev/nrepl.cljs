(defonce counter* (atom 0))

;; Functions you can call from nREPL
(defn inc! [] (swap! counter* inc))
(defn dec! [] (swap! counter* dec))
(defn zero! [] (reset! counter* 0))

#_(repeatedly 10 inc!)
#_(dec!)
#_(zero!)


(defn h [tag attrs & children]
  (let [el (.createElement js/document (name tag))]
    (doseq [[k v] attrs]
      (cond
        (= k :class) (.add (.-classList el) v)
        (= k :style) (when (map? v)
                       (doseq [[style-k style-v] v]
                         (aset (.-style el) (name style-k) style-v)))
        :else (.setAttribute el (name k) v)))
    (doseq [child children]
      (when child
        (cond
          (or (string? child) (number? child)) (.appendChild el (.createTextNode js/document (str child)))
          (seq? child) (doseq [c child] (when c (.appendChild el c)))
          :else (.appendChild el child))))
    el))

(defn counter-html [counter]
  (h :span {:id "counter"} @counter))

(defn display-html [counter]
  (h :div {:style {:border "2px solid black"
                   :padding "1em"
                   :background "#fdfdfd"
                   :border-radius "8px"
                   :max-width "500px"
                   :font-family "sans-serif"}}
     (h :h3 {:style {:marginTop "0" :marginBottom "0.5em"}}
        "✨ Counter: " (counter-html counter) " ✨")
     (h :div {:style {:marginBottom "0.5em"}}
        "Manipulate the counter via nREPL with:")
     (h :code {:style {:background "#f0f0f0"
                       :padding "0.5em"
                       :border-radius "6px"
                       :display "block"
                       :font-family "'Monaco', monospace"
                       :white-space "pre-wrap"}}
        "(inc!) (dec!) (zero!)")))

(let [app (.getElementById js/document "app")]
  (.appendChild app (display-html counter*))
  (add-watch counter* :counter
             (fn [_ _ _ _]
               (let [counter-el (.getElementById js/document "counter")]
                 (.replaceChild (.-parentNode counter-el) (counter-html counter*) counter-el)))))
