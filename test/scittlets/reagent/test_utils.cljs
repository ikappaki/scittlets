(ns scittlets.reagent.test-utils
  (:require [reagent.core :as r]
            [clojure.string :as str]))

(defn copy-to-clipboard [text]
  (.writeText (.-clipboard js/navigator) text))

(defn dependencies+ [tags]
  (let [open?* (r/atom false)
        copied?* (r/atom )
        copy-tooltip* (r/atom false)]
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
         [:span "HTML dependencies"]
         (when open?
           [:div
            [:pre {:style {:border "1px solid #ccc"
                           :margin-top "4px"
                           :padding "8px"
                           :background "#f9f9f9"
                           :position "relative"}}  ;; Add relative positioning for the container
             (str/join "\n" tags)
             ;; Copy icon (positioned at the top-right corner)
             [:button
              {:on-click (fn []
                           (copy-to-clipboard (str/join "\n" tags))
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
