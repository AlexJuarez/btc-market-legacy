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
