(ns scittlets.reagent.test-utils
  (:require [reagent.core :as r]
            [clojure.string :as str]))

(defn copy-to-clipboard [text]
  (.writeText (.-clipboard js/navigator) text))

(def scittlets (js->clj js/scittlets_metadata {:keywordize-keys true}))

(println :scittlest scittlets)

(defn dependencies+ [deps]
  (let [open?* (r/atom false)
        copied?* (r/atom )
        copy-tooltip* (r/atom false)
        sdeps (str/join "\n" deps)]
    (fn []
      (let [open? @open?*
            copied? @copied?*]
        [:div
         {:style {}}
         [:span {:on-click #(swap! open?* not)
                 :style {:color "red"
                         :font-weight "bold"
                         :margin-right "4px"
                         :cursor "pointer"
                         :user-select "none"}}
          (if open? "▾" "▸")]
         [:span "Dependencies"]
         (when open?
           [:div
            [:pre {:style {:border "1px solid #ccc"
                           :margin-top "4px"
                           :padding "8px"
                           :background "#f9f9f9"
                           :position "relative"}}  ;; Add relative positioning for the container
             sdeps
             ;; Copy icon (positioned at the top-right corner)
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

(defn API+ [var-fn]
  (let [{:keys [doc arglists]
         nm :name nmspace :ns} (meta var-fn)
        name-full (str nmspace "/" nm)
        args (->> (first arglists)
                  (str/join " "))]
    [:div {:style {:font-family "Arial, sans-serif" :margin "1em"}}
     [:h2 {:style {:color "#2c3e50"}} name-full]
     [:p [:code (str "Usage: " "[" nm " " args "]") ]]
     [:pre {:style {:background "#f4f4f4" :padding "1em" :border-radius "5px"}}
      doc]]))

(defn info+ [namespace-kw var-fn]
  (let [{:keys [deps]} (get scittlets :scittlets.reagent.mermaid)]
    [:<>
     [API+ var-fn]
     [dependencies+ deps]]))
