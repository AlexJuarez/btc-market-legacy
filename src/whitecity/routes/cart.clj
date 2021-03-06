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
            [whitecity.validator :as v]
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

(defn prep-postages [postages]
  (apply merge (map #(hash-map (:id %) (:price %)) postages)))

(defn prep-listing [{:keys [price lid] :as listing} postages error-hash]
  (let [quantity (or (util/parse-int (cart-get lid :quantity)) 0)
        postage (or (postages (cart-get lid :postage)) 0)
        subtotal (* price quantity)
        total (+ subtotal postage)
        errors (or (get error-hash lid) {})]
    (assoc listing :subtotal subtotal :total total :errors errors)))

(defn prep-listings [listings updates]
  (let [postages (apply merge (map #(prep-postages (:postage %)) listings))]
    (map #(prep-listing % postages updates) listings)))

(defn cart-empty []
  (session/put! :cart {})
  (resp/redirect "/cart"))

(defn cart-update [{:keys [quantity postage]} listings]
  (let [maxes (reduce merge (map #(hash-map (:lid %) (:quantity %)) listings))
        quantities (reduce-kv #(assoc % (util/parse-int %2) {:max (maxes (util/parse-int %2)) :quantity (or (util/parse-int %3) %3)}) {} quantity)
        postages (reduce-kv #(assoc % (util/parse-int %2) {:postage (or (util/parse-int %3) %3)}) {} postage)
        cart-changes (merge-with merge quantities postages)]
    (let [cart (merge-with merge (session/get :cart) cart-changes)
          cart (apply dissoc cart (keep #(if-let [quantity (util/parse-int (:quantity (val %)))] (when (>= 0 quantity) (key %))) cart))]
      (session/put! :cart cart)
      cart)))

(defn get-listings [slug]
  (let [ls (listing/get-in (keys (session/get :cart)))
        cart (cart-update slug ls)
        ls (keep #(if (not (nil? (get cart (:lid %)))) %) ls)
        errors (reduce-kv #(let [e (v/cart-item-validator %3)] (when-not (empty? e) (assoc % %2 e))) {} cart)
        listings (prep-listings ls errors)]
    listings))

(defn cart-view [& slug]
  (let [listings (get-listings (first slug))
        total (reduce + (map #(:total %) listings))
        btc-total (util/convert-price (:currency_id (util/current-user)) 1 total)]
    (layout/render "cart/index.html" (merge {:errors {}
                                             :convert (not (= (:currency_id (util/current-user)) 1))
                                             :total total
                                             :btc-total btc-total
                                             :listings listings} (set-info)))))

(defn cart-checkout [])

(defn cart-submit [{:keys [quantity postage address pin submit] :as slug}]
  (if (= "Update Cart" submit)
    (cart-view slug)
    (let [listings (get-listings slug)
          total (reduce + (map #(:total %) listings))
          btc-total (util/convert-price (:currency_id (util/current-user)) 1 total)
          order (orders/add! (session/get :cart) btc-total address pin (user-id))]
      (if (empty? (:errors order))
        (resp/redirect "/orders")
        (layout/render "cart/index.html" (merge {:errors {}
                                                 :convert (not (= (:currency_id (util/current-user)) 1))
                                                 :total total :btc-total btc-total
                                                 :listings listings} order (set-info)))))))

(defroutes cart-routes
  (context
   "/cart" []
   (GET "/" [] (cart-view))
   (POST "/" {params :params} (restricted (cart-submit params)))
   (GET "/checkout" [] (cart-checkout))
   (GET "/empty" [] (cart-empty))
   (GET "/add/:id" [id] (cart-add id))
   (GET "/:id/remove" [id] (cart-remove id))))
