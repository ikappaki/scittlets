(ns scittlets.reagent.basic-card
  (:require [reagent.core :as r]
            [reagent.dom :as rdom]
            [scittlets.reagent.api-utils :refer [info+ file-open+ demo+]]))

(defonce todos (r/atom [{:id 1 :text "Learn Reagent" :done false}
                        {:id 2 :text "Build something cool" :done false}]))

(defn todo-item [todo]
  (let [{:keys [id]} todo]
    [:li {:key id
          :id (str "todo-" id)}
     [:label
      [:input {:id (str "todo-cb-" id)
               :type "checkbox"
               :checked (:done todo)
               :on-change #(swap! todos
                                  (fn [todos]
                                    (map (fn [t]
                                           (if (= (:id t) (:id todo))
                                             (assoc t :done (not (:done t)))
                                             t))
                                         todos)))}]
      [:span {:id (str "todo-text-" id)
              :style {:text-decoration (when (:done todo) "line-through")}}
       (:text todo)]]]))

(defn todo-list+ []
  [:div
   [:h3 "Todo List"]
   [:div {:style {:margin-bottom "1rem"}}
    "- React test: Clicking the checkbox strikes through the todo item."]
   [:ul
    (for [todo @todos]
      [todo-item todo])]])



(rdom/render
 [:div
  [:section.api {:style {:border-bottom "1px solid #ccc"
                         :padding-bottom "5px"}}
   [info+ :scittlets.reagent {:namespace? false
                              :api-url "https://reagent-project.github.io/docs/master/"}]]

  [:section.test
   [:h4 {:style {:color "#2c3e50"}} [:span "Test " [file-open+ "code" (:file (meta #'todo-list+))]]]
   [todo-list+]]

  [:section.demo
   [demo+ "templates/reagent/reagent_basic.html" "templates/reagent/reagent_basic.cljs"]]]
             ;;[app]
 (.getElementById js/document "app"))

