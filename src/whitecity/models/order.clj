(ns whitecity.models.order
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [clj-time.core :as cljtime]
        [clj-time.coerce :as tc]
        [whitecity.models.user :as user]
        [whitecity.models.postage :as postage]
        [whitecity.models.listing :as listings]
        [whitecity.util :as util]))

(defn check-item [item]
  (let [id (key item)
        quantity (:quantity (val item)) 
        post (:postage (val item)) 
        errors (merge
                (let [error [(when-not (< 0 quantity) "Quantity must be greater than 0") 
                             (when-not (< quantity (listings/max id)) "You can not order more than the max")]]
                  (when-not (empty? error)
                   {:quantity error}))
                (let [error [(when (nil? post) "You need to select a postage")
                             (when (nil? postage/get post (:user_id (listings/get id))) "You need to select a valid postage option")]]
                  (when-not (empty? error)
                    {:postage error})))]
    (when-not (empty? errors)
      {(key item) errors})))

(defn store! [order]
  (insert orders (values order)))

(defn prep [item address user-id]
  (let [id (key item)
        postid (:postage (val item))
        quantity (:quantity (val item))
        post (postage/get postid)
        listing (listings/get id)]
    {:price (:price listing) 
     :postage_price (:price post) 
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
  (let [cart-check {:cart (reduce merge (map check-item cart))}
        address-check (when (empty? address) {:address "You need to enter an address"})
        pin-check (when (empty? (user/get-with-pin user-id pin)) {:pin "Your pin does not match"})
        errors (merge cart-check address-check pin-check)]
    (if (empty? errors)
      (map #(store! (prep % address user-id)) cart)
      {:address address :errors errors})))
