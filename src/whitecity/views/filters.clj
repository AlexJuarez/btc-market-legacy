(ns whitecity.views.filters
  (:require [clojure.string :as s]
            [noir.session :as session])
  (:use selmer.filters
        hiccup.core))

(add-filter! :empty? (fn [x] 
                       (if (string? x) (s/blank? x) (empty? x))))

(add-filter! :count-cart
             (fn [x]
               (:quantity ((session/get :cart) x))))

(add-filter! :postage-cart
             (fn [x]
               (:postage ((session/get :cart) x))))

(defn paginate [page maxpage]
  (loop [c 1 o [page]]
    (if (or (> c 5) (>= (count o) 5))
      o
      (let [pageup (+ page c)
            pagedown (- page c)]
        (recur (inc c) (concat (when (> pagedown 0) [pagedown]) o (when (< pageup maxpage) [pageup])))))))

(add-filter! :pagination
             (fn [x]
               (let [page (if (nil? (:page x)) 1 (:page x))
                     m (:max x)
                     pages (paginate page m)
                     url (:url x)]
                 (html 
                   [:ul.pagination
                    [:li [:a {:href (str url "?page=1")} "&laquo;"]]
                    (map #(-> [:li (if (= page %) [:strong.selected %] [:a {:href (str url "?page=" %)} %])]) pages)
                    [:li [:a {:href (str url "?page=" m)} "&raquo;"]]
                    ]))))
                    ;;[:li [:a {:href (str url "?page=")} ]]]))))

(defn render-tree [tree]
  (let [children (:children tree)]
    (if-not (empty? children)
      [:li 
       [:a.category {:href (str "/market/category/" (:id tree))} (:name tree)] " " [:span.count (:count tree)] 
       [:ul (map render-tree children)]]
      [:li
       [:a.category {:href (str "/market/category/" (:id tree))} (:name tree)] " " [:span.count (:count tree)]])))

(add-filter! :render-tree
             (fn [x]
               (html [:ul (render-tree x)])))

(add-filter! :get-status
             (fn [x]
               (if (= x 0) "processing"
               (if (= x 1) "shipping" "in resolution"))))
