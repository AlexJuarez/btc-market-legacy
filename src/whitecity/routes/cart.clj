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

(defn prep-listing [{:keys [price lid] :as listing} postages updates]
  (let [quantity (or (cart-get lid :quantity) 0)
        postage (or (postages (cart-get lid :postage)) 0)
        subtotal (* price quantity)
        total (+ subtotal postage)
        errors (or (get (:errors updates) lid) {})
        new (or (get (:cart updates) lid) {})]
    (assoc listing :subtotal subtotal :total total :errors errors :new new)))

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
        new-cart (merge-with merge quantities postages)
        errors (reduce-kv #(let [e (v/cart-item-validator %3)] (when-not (empty? e) (assoc % %2 e))) {} new-cart)]
    (if (empty? errors)
      (let [cart  (merge-with merge (session/get :cart) new-cart)]
        (println (keep #(if (or (not (nil? (:quantity (val %)))) (> 0 (:quantity (val %)))) (key %)) cart))
        (session/put! :cart (apply dissoc cart (keep #(if (or (not (nil? (:quantity (val %)))) (> 0 (:quantity (val %)))) (key %)) cart))))
      {:errors errors :cart new-cart})))

(defn cart-view [& slug]
  (let [ls (listing/get-in (keys (session/get :cart)))
        updates (when-not (empty? slug) (cart-update (first slug) ls))
        cart (session/get :cart)
        ls (keep #(if (not (nil? (get cart (:lid %)))) %) ls)
        listings (prep-listings ls updates)
        total (reduce + (map #(:total %) listings))
        btc-total (util/convert-price (:currency_id (util/current-user)) 1 total)]
    (layout/render "users/cart.html" (merge {:errors {}
                                             :convert (not (= (:currency_id (util/current-user)) 1))
                                             :total total
                                             :btc-total btc-total
                                             :listings listings} (set-info)))))

(defn cart-submit [{:keys [quantity postage address pin submit] :as slug}]
  (if (= "Update Cart" submit)
    (cart-view slug)
    (let [ls (listing/get-in (keys (session/get :cart)))
          updates (cart-update slug ls)
          cart (session/get :cart)
          ls (keep #(if (not (nil? (get cart (:lid %)))) %) ls)
          listings (prep-listings ls updates)
          total (reduce + (map #(:total %) listings))
          btc-total (util/convert-price (:currency_id (util/current-user)) 1 total)
          order (orders/add! (session/get :cart) btc-total address pin (user-id))]
      (if (empty? (:errors order))
        (resp/redirect "/orders")
        (layout/render "users/cart.html" (merge {:errors {}
                                                 :convert (not (= (:currency_id (util/current-user)) 1))
                                                 :total total :btc-total btc-total
                                                 :listings listings} order updates (set-info)))))))

(defroutes cart-routes
  (context
   "/cart" []
   (GET "/" [] (cart-view))
   (POST "/" {params :params} (restricted (cart-submit params)))
   (GET "/empty" [] (cart-empty))
   (GET "/add/:id" [id] (cart-add id))
   (GET "/:id/remove" [id] (cart-remove id))))
