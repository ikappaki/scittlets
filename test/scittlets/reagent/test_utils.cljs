(ns scittlets.reagent.test-utils
  (:require [reagent.core :as r]
            [clojure.string :as str]))

(defn copy-to-clipboard [text]
  (.writeText (.-clipboard js/navigator) text))

(def scittlets (js->clj js/scittlets_metadata {:keywordize-keys true}))

;;(println :scittlest scittlets)

(defn- html-escape [s]
  (-> (clojure.string/replace s "&" "&amp;")
      (clojure.string/replace "<" "&lt;")
      (clojure.string/replace ">" "&gt;")
      (clojure.string/replace "\"" "&quot;")
      (clojure.string/replace "'" "&#39;")))

(defn- url-text-open [url]
  (let [window-name url
        w (js/window.open "about:blank" window-name)]
    (-> (js/fetch url)
        (.then #(.text %))
        (.then (fn [text]
                 (let [doc (.-document w)]
                   (.open doc)
                   (.write doc (str "<title>" window-name "</title>"
                                    "<pre>"(html-escape text)"</pre>"))
                   (.close doc)))))))

(defn file-open+ [label url]
  [:button {:on-click #(url-text-open url)
            :style {:all "unset"
                    :cursor "pointer"
                    :font-size "10px"
                    :padding-right "5px"
                    :color "blue"
                    :text-decoration "underline"}}
   (str "[" label "]")])

(defn dependencies+ [deps]
  (let [open?* (r/atom false)
        copied?* (r/atom )
        copy-tooltip* (r/atom false)
        sdeps (str/join "\n" deps)]
    (fn []
      (let [open? @open?*
            copied? @copied?*]
        [:div
         [:span {:on-click #(swap! open?* not)
                 :style {:cursor "pointer"
                         :user-select "none"}}
          [:span {:style {:color "red"
                          :font-weight "bold"
                          :margin-right "4px"}}
           (if open? "▾" "▸")]
          (count deps) " Dependencies" (when-not open? " ...")]
         (when open?
           [:div
            [:pre {:style {:border "1px solid #ccc"
                           :margin-top "4px"
                           :padding "8px"
                           :background "#f9f9f9"
                           :position "relative"}}
             sdeps
             [:button
              {:on-click (fn []
                           (copy-to-clipboard sdeps)
                           (reset! copied?* true))
               :title (if copied? "Copied" "Copy")
               :style {:position "absolute"
                       :top "8px"
                       :right "8px"
                       :border "none"
                       :background "transparent"
                       :cursor "pointer"
                       :font-size "18px"
                       :color "#007bff"
                       :transition "color 0.3s ease"}}
              (if copied? "✔️" "📋")]]])]))))

(defn API+ [var-fn home see]
  (let [{:keys [arglists doc file]
         nm :name nmspace :ns} (meta var-fn)
        name-full (str nmspace "/" nm)
        args (->> (first arglists)
                  (str/join " "))]
    [:div {:style {:font-family "Arial, sans-serif" :margin "1em"}}
     [:h2 {:style {:color "#2c3e50"}} (into [:span name-full
                                             "  " [:a {:href home :target "_blank"
                                                       :style {:padding-right "5px"
                                                               :font-size "15px"}}
                                                   "🏠"]
                                             [file-open+ "code" file]]
                                            (map (fn [[label url]] (vector  :a {:href url :target "_blank"
                                                                                :style {:padding-right "5px"
                                                                                        :font-size "10px"}}
                                                                            (str "[" (name label) "]"))) see))]
     [:p [:code (str "Usage: " "[" nm " " args "]") ]]
     [:pre {:style {:background "#f4f4f4" :padding "1em" :border-radius "5px"}}
      doc]]))

(defn info+ [namespace-kw var-fn]
  (let [{:keys [deps home see]} (get scittlets namespace-kw)]
    [:<>
     [API+ var-fn home see]
     [dependencies+ deps]]))
