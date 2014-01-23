(ns whitecity.models.order
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.models.user :as user]
        [whitecity.models.postage :as postage]
        [whitecity.models.listing :as listings]
        [noir.session :as session]
        [whitecity.util :as util]))

;;Order status numbers
;;0 - processing
;;1 - shipping
;;2 - resolution
;;3 - finalized

(defn all [id]
  (select orders
    (with sellers (fields :login :alias))
    (where (and (= :user_id id) (or (< :status 3) (not :reviewed))))))

(defn sold 
  ([id]
   (select orders
    (with users (fields :login :alias))
    (with postage (fields [:title :postage_title]))
    (where {:seller_id id})))
 ([status id]
  (select orders
    (with users (fields :login :alias))
    (with postage (fields [:title :postage_title]))
    (where {:seller_id id :status status}))))

(defn check-item [item]
  (let [id (key item)
        quantity (:quantity (val item)) 
        post (:postage (val item)) 
        listing (listings/get id)
        errors (merge
                (let [error (reduce merge [(when-not (< 0 quantity) ["Quantity must be greater than 0"]) 
                             (when-not (<= quantity (:quantity listing)) ["You can not order more than the max"])])]
                  (when-not (empty? error)
                   {:quantity error}))
                (let [error (reduce merge (when (nil? (postage/get post (:user_id listing))) ["You need to select a valid postage option"]))]
                  (when-not (empty? error)
                    {:postage error})))]
    (when-not (empty? errors)
      {id errors})))

(defn store! [order]
  (transaction
    (insert orders (values order))
    (update listings 
            (set-fields {:quantity (raw (str "quantity - " (:quantity order)))})
            (where {:id (:listing_id order)}))))

(defn prep [item address user-id]
  (let [id (key item)
        postid (:postage (val item))
        quantity (:quantity (val item))
        post (postage/get postid)
        listing (listings/get id)]
    {:price (:price listing) 
     :postage_price (:price post) 
     :postage_title (:title post)
     :quantity quantity
     :hedged (:hedged listing)
     :title (:title listing)
     :address address
     :seller_id (:user_id listing)
     :currency_id (:currency_id listing)
     :listing_id id
     :postage_id postid
     :user_id user-id
     :status 0}))

(defn add! [cart address pin user-id]
  (let [cart-check (let [cart (reduce merge (map check-item cart))] (when-not (empty? cart) {:cart cart}))
        address-check (when (empty? address) {:address "You need to enter an address"})
        pin-check (when (empty? (user/get-with-pin user-id pin)) {:pin "Your pin does not match"})
        postage (some false? (map #(not (nil? (:postage (val %)))) cart-check))
        postage-error (when postage {:postage postage})
        errors (merge cart-check postage-error address-check pin-check)]
    (if (empty? errors)
      (do 
        (session/put! :cart {}) 
        (util/user-clear user-id)
        (apply #(store! (prep % address user-id)) cart))
      {:address address :errors errors})))

(defn update-sales [sales seller-id status]
  (update orders
          (set-fields {:status status})
          (where {:seller_id seller-id :id [in sales]})))

(defn finalize [id user-id]
  (update orders
          (set-fields {:status 3})
          (where {:user_id user-id :id (util/parse-int id)})))

(defn reject-sales [sales seller-id]
  (let [o (select orders
                  (where {:seller_id seller-id :id [in sales]}))]
  (dorun (map #(update listings (set-fields {:quantity (raw (str "quantity + " (:quantity %)))}) (where {:id (:listing_id %) :user_id seller-id})) o))
  (delete orders
          (where {:seller_id seller-id :id [in sales]}))
  (util/user-clear seller-id)))

(defn count [id]
  (:cnt (first (select orders
    (aggregate (count :*) :cnt)
    (where {:user_id id :status [in (list 0 1 2)]})))))

(defn count-sales [id]
  (:cnt (first (select orders
    (aggregate (count :*) :cnt)
    (where {:seller_id id :status 0})))))
