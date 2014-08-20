(ns whitecity.routes.cart
  (:use
   [compojure.core :only [GET POST defroutes context]]
   [noir.util.route :only (restricted)]
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
  (let [cart (or (session/get :cart) {})
        id (util/parse-int id)]
    (when (< (count cart) cart-limit) ;;limit the size of the cart... aka they should only be able to have like 100 items
      (session/update-in! [:cart id :quantity] (fnil inc 0)))
    (resp/redirect "/cart")))

(defn cart-remove
  [id]
  (let [cart (session/get :cart)
        id (util/parse-int id)]
    (session/put! :cart (dissoc cart id)))
  (resp/redirect "/cart"))

(defn cart-get
  [id key]
  (session/get-in [:cart id key]))

(defn postage-get-price
  [id postages]
  (if (nil? id)
    0
    (:price (first (filter #(= id (:id %)) postages)))))

(defn prep-postages [postages]
  (apply merge (map #(hash-map (:id %) (:price %)) postages)))

(defn prep-listing [{:keys [price lid] :as listing} postages]
  (let [quantity (or (cart-get lid :quantity) 0)
        postage (or (postages lid) 0)
        subtotal (* price quantity)
        total (+ subtotal postage)]
    (conj listing {:subtotal subtotal :total total})))

(defn prep-listings [listings]
  (let [postages (apply merge (map #(prep-postages (:postage %)) listings))]
    (map #(prep-listing % postages) listings)))

(defn cart-empty []
  (session/put! :cart {})
  (resp/redirect "/cart"))

(defn cart-view []
  (let [ls (listing/get-in (keys (session/get :cart)))
        listings (prep-listings ls)
        total (reduce + (map #(:total %) listings))
        btc-total (util/convert-price (:currency_id (util/current-user)) 1 total)]
    (layout/render "users/cart.html" (merge {:errors {} :convert (not (= (:currency_id (util/current-user)) 1)) :total total :btc-total btc-total :listings listings} (set-info)))))

(defn cart-update [{:keys [quantity postage address pin submit] :as slug}]
  (session/put! :cart
                (let [cart (reduce merge
                                   (map #(hash-map (key %) {:quantity (or (util/parse-int (quantity (str (key %)))) 0)
                                                            :postage (util/parse-int (postage (str (key %))))})
                                        (session/get :cart)))]
                  (select-keys cart (for [[k v] cart :when (> (:quantity v) 0)] k))))
  (if (= "Update Cart" submit)
    (cart-view)
    (let [ls (listing/get-in (keys (session/get :cart)))
          listings (prep-listings ls)
          total (reduce + (map #(:total %) listings))
          btc-total (util/convert-price (:currency_id (util/current-user)) 1 total)
          order (orders/add! (session/get :cart) btc-total address pin (user-id))]
      (if (empty? (:errors order))
        (resp/redirect "/orders")
        (layout/render "users/cart.html" (merge {:errors {} :convert (not (= (:currency_id (util/current-user)) 1)) :total total :btc-total btc-total :listings listings} order (set-info)))))))

(defroutes cart-routes
  (context
   "/cart" []
   (GET "/" [] (cart-view))
   (POST "/" {params :params} (restricted (cart-update params)))
   (GET "/empty" [] (cart-empty))
   (GET "/add/:id" [id] (cart-add id))
   (GET "/:id/remove" [id] (cart-remove id))))
