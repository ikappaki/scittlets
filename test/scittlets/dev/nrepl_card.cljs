(ns scittlets.dev.nrepl-card)

(defonce catalog* (atom nil))
(defonce file-contents* (atom {}))

(defn h [tag attrs & children]
  (let [el (.createElement js/document (name tag))]
    (doseq [[k v] attrs]
      (cond
        ;; If key starts with "on" and value is a function, add event listener
        (and (string? (name k))
             (.startsWith (name k) "on")
             (fn? v))
        (let [event-name (.toLowerCase (subs (name k) 2))] ;; e.g. :onclick → "click"
          (.addEventListener el event-name v))

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

(def info-styles
  {:background "#f8f9fa"
   :border "1px solid #e9ecef"
   :padding "5px"
   :border-radius "10px"
   :margin-bottom "20px"
   :font-family "Arial, sans-serif"})

(def version-styles
  {:font-size "0.7em"
   :color "#6c757d"
   :font-weight "normal"})

(def deps-styles
  {:font-family "Monaco, monospace"
   :background "#e9ecef"
   :padding "8px"
   :border-radius "5px"
   :font-size "0.9em"
   :color "#495057"})

(defn info-get [catalog scittlet]
  (get @catalog scittlet))

(defn dependencies-get [catalog scittlet] 
  (if-let [info (info-get catalog scittlet)]
    (get info "deps")
    ["loading..."]))  ; fallback

(defn see-get [catalog scittlet]
  (when-let [info (info-get catalog scittlet)]
    (get info "see")))

(defn files-get [catalog scittlet]
  (when-let [info (info-get catalog scittlet)]
    (get info "files")))

(defn file-contents-load! [catalog scittlet file-contents]
  (when-let [files (files-get catalog scittlet)]
    (doseq [{:strs [src dest]} files]
      (when-not (get @file-contents dest)
        (-> (js/fetch src)
            (.then #(.text %))
            (.then #(swap! file-contents assoc dest %))
            (.catch #(swap! file-contents assoc dest (str "Could not load file :" src " " %))))))))

(defn version-get []
  (if-let [meta-tag (-> js/document
                        (.querySelector "meta[name='scittlets.dev.nrepl.version']"))]
    (.-content meta-tag)
    "unknown"))

(defn header-html [catalog scittlet]
  (h :h3 {:style {:margin-top "0" :color "#495057"}}
     "📦 "
     scittlet
     " "
     (h :span {:style version-styles} (version-get))
     (h :span {:style version-styles} (h :a {:href "https://github.com/ikappaki/scittlets"
                                             :target "_blank"
                                             :style {:margin-left "8px"}} "🏠"))

     (when-let [see-links (see-get catalog scittlet)]
       (h :span {:style {:font-size "0.7em"}}
          (doall (map (fn [[name url]]
                        (h :a {:href url :target "_blank"
                               :style {:color "#007bff" :text-decoration "none" :margin-left "8px"}}
                           name))
                      see-links))))))

(defn with-copy-button [text-to-copy & content]
  (let [id (str "copy-btn-" (random-uuid))]
    (h :div {:style {:position "relative"}}
       ;; copy button absolutely positioned top-right
       (h :button
          {:id id
           :style {:font-size "1em"
                   :padding "2px 6px"
                   :border "none"
                   :background "transparent"
                   :cursor "pointer"
                   :position "absolute"
                   :top "0px"
                   :right "0px"
                   :z-index 10}
           :title "Copy to clipboard"
           :onclick
           (fn [_event]
             (.writeText (.-clipboard js/navigator) text-to-copy)
             (let [btn (.getElementById js/document id)
                   old-text (.-textContent btn)]
               (set! (.-textContent btn) "✔️")
               (js/setTimeout
                 (fn [] (set! (.-textContent btn) old-text))
                 1500)))}
          "📋")

       ;; your content goes here
       (apply h :div {} content))))
;;✔️


(defn dependencies-html [catalog scittlet]
  (let [deps (dependencies-get catalog scittlet)
        deps-str (clojure.string/join "\n" deps)]
    (h :details {}
       (h :summary {:style {:cursor "pointer" :font-weight "bold"}} (str "Dependencies (" (count deps) ")"))
       (h :div {:style deps-styles}
          (with-copy-button deps-str
            (doall (interpose (h :br {})
                              (map #(h :span {} %)
                                   deps))))))))

(defn files-html [catalog scittlet file-contents]
  (when-let [files (files-get catalog scittlet)]
    (h :details {:id "files"}
       (h :summary {:style {:margin-top "15px" :cursor "pointer" :font-weight "bold"}}
          (str "files (" (count files) ")"))
       (doall (map (fn [{:strs [dest]}]
                     (h :div {;;:style {:margin-bottom "15px"}
                              }
                        (h :div {:style {:font-weight "bold" :margin "5px"}} dest)
                        (h :pre {:style {:font-family "Monaco, monospace" :background "#f8f9fa"
                                         :padding "10px" :border-radius "5px" :font-size "0.85em"
                                         :overflow-x "auto" :border "1px solid #e9ecef"}}
                           (if-let [contents (get @file-contents dest)]
                             (with-copy-button contents contents)
                             "Loading..."))))
                   files)))))

(defn nrepl-html []
  (h :div {}
     (h :p {}
        "This Scittlet includes an "
        (h :code {} "nrepl-server.clj")
        " script, which starts an nREPL server for interacting with the environment.")

     (h :p {} "To launch the server using "
        (h :a {:href "https://github.com/babashka/babashka#installation" :target "_blank"} "Babashka")
        " run:")
     (h :pre {} (h :code {} "bb nrepl-server.clj [--port PORT]"))
     (h :p {}
        "The default nREPL port is " (h :code {} "1339") ".")

     (h :p {}
        "Once running, you can connect to it from a ClojureScript-enabled editor like "
        (h :strong {} "Emacs")
        " or "
        (h :strong {} "VSCode")
        ".")

     (h :div {}
        (h :h4 {}  (h :a {:href "https://docs.cider.mx/cider/index.html" :target "_blank"} "CIDER") " (Emacs)")
        (h :ul {}
           (h :li {} "Run " (h :code {} "M-x cider-connect-cljs"))
           (h :li {} "Set Host to " (h :code {} "localhost"))
           (h :li {} "Set Port to " (h :code {} "1339"))
           (h :li {} "Select ClojureScript REPL type: " (h :code {} "nbb"))))

     (h :div {}
        (h :h4 {} (h :a {:href "https://calva.io/" :target "_blank"} "Calva") " (VSCode)")
        (h :ul {}
           (h :li {} "Press " (h :strong {} "Ctrl-Alt-P") " to open the Command Palette.")
           (h :li {} "Select " (h :code {} "Calva: Connect to a Running REPL Server"))
           (h :li {} "Select Project Type/Connect Sequence: " (h :code {} "nbb"))
           (h :li {} "Enter " (h :code {} "localhost:1339"))))))

(defn el-replace! [id content]
  (when-let [old (.getElementById js/document id)]
    (.replaceChild (.-parentNode old) content old)))

(defn load-catalog! [catalog]
  (-> (js/fetch "catalog.json")
      (.then #(.json %))
      (.then #(reset! catalog (js->clj %)))
      (.catch #(println :load-catalog/error (str %)))))

(defn info-html [catalog scittlet doc file-contents]
  (h :div {:id "info" :style info-styles}
     (header-html catalog scittlet)
     doc
     (dependencies-html catalog scittlet)
     (files-html catalog scittlet file-contents)))

(defn info-el-append [app scittlet doc]
  (.appendChild app (info-html catalog* scittlet doc file-contents*))
  (js/requestAnimationFrame
   (fn []
     (add-watch catalog* :info
                (fn [_ _ _ _]
                  (el-replace! "info" (info-html catalog* scittlet doc file-contents*))
                  (js/requestAnimationFrame
                   #(file-contents-load! catalog* scittlet file-contents*))))
     (add-watch file-contents* :files
                (fn [_ _ _ _]
                  (el-replace! "files" (files-html catalog* scittlet file-contents*))))

     (load-catalog! catalog*))))

(let [app (.getElementById js/document "app")]
  (set! (.-innerHTML app) "")
  (info-el-append app "scittlets.dev.nrepl" (nrepl-html))
  (.appendChild app (h :h4 {} "Demo"
                       (h :iframe {:name "nrepl" :src "examples/dev/nrepl.html"
                                   :style {"height" "45vh"
                                           "margin-top" "1em"
                                           "width" "99%"
                                           "border" "5px solid #add8e6"
                                           "border-radius" "5px"
                                           "box-shadow" "0 4px 6px rgba(0, 0, 0, 0.1)"}}))))
