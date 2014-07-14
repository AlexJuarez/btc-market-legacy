(ns whitecity.routes.cart
  (:use
    [compojure.core :only [GET POST]]
    [noir.util.route :only (def-restricted-routes)]
    [whitecity.helpers.route])
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.postage :as postage]
            [whitecity.models.listing :as listing]
            [whitecity.models.currency :as currency]
            [whitecity.models.order :as orders]
            [noir.response :as resp]
            [noir.session :as session]
            [whitecity.util :as util]))

(def cart-limit 100)

(defn cart-add
  "add a item to the cart"
  [id]
  (let [cart (session/get :cart) id (util/parse-int id)]
    (when (< (count cart) cart-limit) ;;limit the size of the cart... aka they should only be able to have like 100 items
      (session/put! :cart (assoc cart id {:quantity (if-let [item (cart id)] (inc (:quantity item)) 1)
                                                           :postage (if-let [item (cart id)] (:postage item))})))
  (resp/redirect "/market/cart")))

(defn cart-remove
  [id]
  (let [cart (session/get :cart)]
    (session/put! :cart (dissoc cart (util/parse-int id))))
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

(defn cart-view []
  (let [ls (listing/get-in (keys (session/get :cart)))
        listings (if-not (empty? ls) (map #(let [subtotal (* (:price %) (cart-get (:lid %) :quantity))
                             total (+ subtotal (postage-get-price (cart-get (:lid %) :postage) (:postage %)))]
                         (conj % {:subtotal subtotal :total total})) ls))
        total (reduce + (map #(:total %) listings))]
    (layout/render "users/cart.html" (merge {:errors {} :total total :listings listings} (set-info)))))

(defn cart-update [{:keys [quantity postage address pin submit] :as slug}]
  (session/put! :cart
                (let [cart (reduce merge
                  (map #(hash-map (key %) (merge (val %) {:quantity (util/parse-int (quantity (str (key %))))
                                                          :postage (util/parse-int (postage (str (key %))))}))
                          (session/get :cart)))] (select-keys cart (for [[k v] cart :when (> (:quantity v) 0)] k))))
  (if (= "Update Cart" submit)
    (cart-view)
    (let [ls (listing/get-in (keys (session/get :cart)))
          listings (if-not (empty? ls) (map #(let [subtotal (* (:price %) (cart-get (:lid %) :quantity))
                               total (+ subtotal (postage-get-price (cart-get (:lid %) :postage) (:postage %)))]
                           (conj % {:subtotal subtotal :total total})) ls))
          total (reduce + (map #(:total %) listings))
          order (orders/add! (session/get :cart) (util/convert-currency 1 total) address pin (user-id))]
      (if (empty? (:errors order))
        (resp/redirect "/market/orders")
        (layout/render "users/cart.html" (merge {:errors {} :total total :listings listings} order (set-info)))))))

(def-restricted-routes cart-routes
    (GET "/market/cart" [] (cart-view))
    (GET "/market/cart/empty" [] (cart-empty))
    (POST "/market/cart" {params :params} (cart-update params))
    (GET "/market/cart/add/:id" [id] (cart-add id))
    (GET "/market/cart/:id/remove" [id] (cart-remove id)))
