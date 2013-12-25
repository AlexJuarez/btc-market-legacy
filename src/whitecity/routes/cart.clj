(ns whitecity.routes.cart
  (:use compojure.core
        noir.util.route
        whitecity.helpers.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.postage :as postage]
            [whitecity.models.listing :as listing]
            [whitecity.models.currency :as currency]
            [whitecity.models.order :as order]
            [noir.response :as resp]
            [noir.session :as session]
            [whitecity.util :as util]))

(defn cart-add
  "add a item to the cart"
  [id]
  (session/put! :cart (conj (session/get :cart) {(util/parse-int id) {:quantity 1 :postage nil}}))
  (resp/redirect "/market/cart"))

(defn cart-get
  [id key]
   (key ((session/get :cart) id)))

(defn postage-get-price
  [id postages]
  (if (nil? id)
    0
    (:price (first (filter #(= id (:id %)) postages)))))

(defn cart-empty []
  (session/put! :cart {})
  (resp/redirect "/market/cart"))

(defn cart-view [& option]
  (let [ls (listing/get-in (keys (session/get :cart)))
        listings (if-not (empty? ls) (map #(let [subtotal (* (:price %) (cart-get (:lid %) :quantity))
                             total (+ subtotal (postage-get-price (cart-get (:lid %) :postage) (:postage %)))] 
                         (conj % {:subtotal subtotal :total total})) ls))]
    (layout/render "users/cart.html" (merge {:errors {} :listings listings} (first option) (set-info)))))

(defn cart-update [{:keys [quantity postage address pin submit] :as slug}]
  (session/put! :cart 
                (let [cart (reduce merge 
                  (map #(hash-map (key %) (merge (val %) {:quantity (util/parse-int (quantity (str (key %)))) 
                                                          :postage (util/parse-int (postage (str (key %))))})) 
                          (session/get :cart)))] (select-keys cart (for [[k v] cart :when (> (:quantity v) 0)] k))))
  (if (= "Update Cart" submit)
    (cart-view)
    (let [order (order/add! (session/get :cart) address pin (user-id))]
      (if (empty? (:errors order))
        (resp/redirect "/market/orders")
        (cart-view order)))))

(def-restricted-routes cart-routes
    (GET "/market/cart" [] (cart-view))
    (GET "/market/cart/empty" [] (cart-empty))
    (POST "/market/cart" {params :params} (cart-update params))
    (GET "/market/cart/add/:id" [id] (cart-add id)))
