(ns whitecity.models.order
  (:refer-clojure :exclude [get count])
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.models.postage :as postage]
        [whitecity.models.listing :as listings]
        [noir.session :as session]
        [whitecity.util :as util]))

;;Order status numbers
;;0 - processing
;;1 - shipping
;;2 - resolution
;;3 - finalized

(def statuses [:new :ship :resolution :finalize])

(defn get-order [id user-id]
  (first (select orders
                 (with sellers (fields :login :alias))
                 (where {:id (util/parse-int id) :user_id user-id}))))

(defn get-sale [id seller-id]
  (first (select orders
                 (with users (fields :login :alias))
                 (where {:id (util/parse-int id) :seller_id seller-id}))))
(defn all [id]
  (select orders
    (with sellers (fields :login :alias))
    (where (and (= :user_id id) (or (< :status 3) (not :reviewed))))))

(defn sold 
  ([id page per-page]
   (select orders
    (with users (fields :login :alias))
    (with postage (fields [:title :postage_title]))
    (where {:seller_id id})
    (offset (* (- page 1) per-page))
    (limit per-page)))
 ([status id page per-page]
  (select orders
    (with users (fields :login :alias))
    (with postage (fields [:title :postage_title]))
    (where {:seller_id id :status status})
    (offset (* (- page 1) per-page))
    (limit per-page))))

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

(defn store! [order user-id pin]
  (let [item-cost (util/convert-price (:currency_id order) 1 (:price order))
        postage-cost (util/convert-price (:postage_currency order) 1 (:postage_price order))
        cost (+ item-cost postage-cost)
        {:keys [user_id seller_id listing_id quantity]} order
        escr {:from user_id :to seller_id :currency_id 1 :amount cost :status "hold"}
        audit {:user_id user-id :role "purchase" :amount (* -1 cost)}]
    (util/update-session seller_id :sales)
    (let [order (transaction
      (insert audits (values audit))
      (update users (set-fields {:btc (raw (str "btc - " cost))}) (where {:id user-id :pin pin}))
      (update listings 
              (set-fields {:updated_on (raw "now()") :quantity (raw (str "quantity - " quantity))})
              (where {:id listing_id}))
      (insert orders (values order)))]
      (if (not (empty? order));;TODO: what does korma return when it errors out?
      (insert escrow (values (assoc escr :order_id (:id order))))))))

(defn prep [item address user-id]
  (let [id (key item)
        postid (:postage (val item))
        quantity (:quantity (val item))
        post (postage/get postid)
        listing (listings/get id)]
    {:price (:price listing) 
     :postage_price (:price post) 
     :postage_title (:title post)
     :postage_currency (:currency_id post)
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

(defn add! [cart total address pin user-id]
  (let [pin (util/parse-int pin)
        user (first (select users (fields :login :btc) (where {:id user-id :pin pin})))
        cart-check (let [cart (reduce merge (map check-item cart))] (when-not (empty? cart) {:cart cart}))
        address-check (when (empty? address) {:address "You need to enter an address"})
        pin-check (when (empty? user) {:pin "Your pin does not match"})
        postage (some false? (map #(not (nil? (:postage (val %)))) cart-check))
        postage-error (when postage {:postage postage})
        insufficient-funds (when (< (:btc user) total) {:total "insufficient funds"})
        errors (merge cart-check postage-error address-check pin-check insufficient-funds)]
    (if (empty? errors)
      (do 
        (session/put! :cart {}) 
        (util/update-session user-id :orders :sales)
        (apply #(store! (prep % address user-id) user-id pin) cart))
      {:address address :errors errors})))

(defn update-sales [sales seller-id status]
  (if (= status 1) (util/update-session seller-id :sales :orders))
  (let [values {:status status :updated_on (raw "(now())")}
        values (if (= status 1) (assoc values :auto_finalize (raw "(now() + interval '17 days')")) values)]
  (update orders
          (set-fields values)
          (where {:seller_id seller-id :id [in sales]}))))

;;race condition possible nooo
(defn finalize [id user-id]
  (util/update-session user-id :orders :sales)
  (let [id (util/parse-int id) 
        {seller_id :seller_id listing_id :listing_id} (first (select orders (fields :seller_id) (where {:id id :user_id user-id :status [not= 3]})))
        {amount :amount currency_id :currency_id} (first (select escrow (where {:order_id id :from user-id :status "hold"})))
        amount (util/convert-price currency_id 1 amount)
        audit {:amount amount :user_id user-id :role "sale"}]
      (transaction
        (insert audits (values audit))
        (update users (set-fields {:btc (raw (str "btc + " amount))}) (where {:id seller_id}))
        (update escrow (set-fields {:status "done" :updated_on (raw "now()")}) (where {:order_id id}))
        (update listings (set-fields {:sold (raw "sold + 1") :updated_on (raw "now()")}) (where {:id listing_id}))
        (update orders
              (set-fields {:status 3 :updated_on (raw "now()")})
              (where {:user_id user-id :id (util/parse-int id)})))))

(defn resolution [id user-id]
  (util/update-session user-id :orders :sales)
  (update orders
          (set-fields {:status 2 :updated_on (raw "now()")})
          (where {:user_id user-id :id (util/parse-int id)})))

(defn reject-sales [sales seller-id]
  (util/update-session seller-id :sales :orders)
  (let [o (select orders
                  (where {:seller_id seller-id :id [in sales]}))]
    (dorun (map #(update listings (set-fields {:quantity (raw (str "quantity + " (:quantity %)))}) (where {:id (:listing_id %) :user_id seller-id})) o))
    (delete orders
            (where {:seller_id seller-id :id [in sales]}))
    (util/update-session seller-id)))

(defn count [id]
  (:cnt (first (select orders
    (aggregate (count :*) :cnt)
    (where {:user_id id :status [in (list 0 1 2)]})))))

;;map this into vector of status cnt's into a hash
(defn count-sales 
  ([id]
   (let [sales
     (into {}
          (map #(vector (statuses (:status %)) (:cnt %))
            (select orders
              (fields :status)
              (aggregate (count :*) :cnt)
              (where {:seller_id id})
              (group :status))))]
     (assoc sales :total (reduce + (vals sales))))))
