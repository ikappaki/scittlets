(ns basic
  (:require [reagent.core :as r]
            [reagent.dom :as dom]))


(defonce state* (r/atom {:count 0}))

(defn counter []
  (let [count (:count @state*)]
    [:div {:style {:font-family "Arial, sans-serif"
                   :text-align "center"
                   :margin-top "50px"}}
     [:h1 "🎉 Welcome to Reagent! 🎉"]
     [:p "Click the button to increase the fun!"]
     [:h2 {:style {:color "#007acc"}} "Count: " count]
     [:button {:style {:padding "10px 20px"
                       :font-size "16px"
                       :cursor "pointer"
                       :background "#00c896"
                       :color "white"
                       :border "none"
                       :border-radius "5px"}
               :on-click #(swap! state* update :count inc)}
      "Add 🎈"]]))

(dom/render [counter state*] (.getElementById js/document "app"))
