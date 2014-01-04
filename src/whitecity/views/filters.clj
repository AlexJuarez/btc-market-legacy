(ns whitecity.views.filters
  (:require [clojure.string :as s]
            [noir.session :as session])
  (:use selmer.filters))

(add-filter! :empty? (fn [x] 
                       (if (string? x) (s/blank? x) (empty? x))))

(add-filter! :count-cart
             (fn [x]
               (:quantity ((session/get :cart) x))))

(add-filter! :postage-cart
             (fn [x]
               (:postage ((session/get :cart) x))))

(defn render-tree [tree]
  (let [children (:children tree)]
    (if-not (empty? children)
      (into [] (flatten (conj [] "<li><a class='category' href='/market/category/" (:id tree) "'>" (:name tree) "</a> <span class='count'>" (:count tree) "</span><ul>" (into [] (mapcat render-tree children)) "</ul></li>")))
      (conj [] "<li><a class='category' href='/market/category/" (:id tree) "'>" (:name tree) "</a> <span class='count'>" (:count tree) "</span></li>"))))

(add-filter! :render-tree
             (fn [x]
              (apply str (flatten (render-tree x)))))

(add-filter! :get-status
             (fn [x]
               (if (= x 0) "processing"
               (if (= x 1) "shipping" "in resolution"))))
