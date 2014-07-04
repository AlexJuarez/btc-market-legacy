(ns whitecity.models.order
  (:refer-clojure :exclude [get count])
  (:use [korma.db :only (transaction)]
        [korma.core]
        [environ.core :only [env]]
        [whitecity.db])
  (:require
        [whitecity.models.postage :as postage]
        [whitecity.models.listing :as listings]
        [whitecity.validator :as v]
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
  (let [currency_id (if (:hedged order) (:currency_id order) 1)
        item-cost (util/convert-price (:currency_id order) currency_id (:price order))
        postage-cost (util/convert-price (:postage_currency order) currency_id (:postage_price order))
        cost (+ (* (:quantity order) item-cost) postage-cost)
        btc_cost (util/convert-price currency_id 1 cost) ;;use an env flag for this
        {lq :listing_quantity lp :listing_pubic cat_id :category_id} order
        {:keys [user_id seller_id listing_id quantity] :as order} (dissoc order :listing_quantity :listing_pubic :category_id)
        escr {:from user_id :hedged (:hedged order) :to seller_id :currency_id currency_id :amount cost :btc_amount btc_cost :status "hold"}
        audit {:user_id user-id :role "purchase" :amount (* -1 btc_cost)}]
    (util/update-session seller_id :sales)
    (let [order (transaction
      (insert audits (values audit))
      (update users (set-fields {:btc (raw (str "btc - " btc_cost))}) (where {:id user-id :pin pin}))
      (update listings
              (set-fields {:updated_on (raw "now()") :quantity (raw (str "quantity - " quantity))})
              (where {:id listing_id}))
      (if (and lp (<= lq 0)) (update category (set-fields {:count (raw "count - 1")}) (where {:id cat_id})))
      (insert orders (values order)))]
      (if (not (empty? order));;TODO: what does korma return when it errors out?
      (insert escrow (values (assoc escr :order_id (:id order))))))))

(defn cancel!
  ([{:keys [id seller_id user_id listing_id quantity] :as order}]
   (println order)
   (let [escr (update escrow (set-fields {:status "refunded"}) (where {:status "hold" :order_id id}))
         listing (update listings
                         (set-fields {:updated_on (raw "now()") :quantity (raw (str "quantity + " quantity))})
                         (where {:id listing_id}))]
     (println escr)
     (when-not (empty? escrow)
       (util/update-session seller_id :sales :orders)
       (util/update-session user_id :sales :orders)
       (transaction
        (insert audits (values {:user_id user_id :role "refund" :amount (:btc_amount escr)}))
        (update users (set-fields {:btc (raw (str "btc + " (:btc_amount escr)))}) (where {:id user_id}))
        (if (and (:public listing) (<= (- (:quantity listing) quantity) 0))
          (update category (set-fields {:count (raw "count + 1")}) (where {:id (:category_id listing)})))
        (update orders (set-fields {:status 3 :reviewed true}) (where {:id id}))))))
  ([id user-id]
   (let [order (first (select orders (where {:id id :status 0})))]
     (if (or (= (:seller_id order) user-id)
             (= (:user_id order) user-id))
       (cancel! order)))))

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
     :hedge_fee (:hedge_fee listing)
     :quantity quantity
     :listing_quantity (:quantity listing) ;;this field gets removed
     :listing_pubic (:public listing) ;;placeholder
     :category_id (:category_id listing)
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
  (let [cart-check (let [cart (reduce merge (map check-item cart))] (when-not (empty? cart) {:cart cart}))
        postage (some false? (map #(not (nil? (:postage (val %)))) cart-check))
        postage-error (when postage {:postage postage})
        check (v/cart-validator {:address address :pin pin :total total :user_id user-id})
        errors (merge cart-check postage-error check)]
    (if (empty? errors)
      (do
        (session/put! :cart {})
        (util/update-session user-id :orders :sales)
        (doall (map #(store! (prep % address user-id) user-id pin) cart)))
      {:address address :errors errors})))

(defn update-sales [sales seller-id status]
  (if (= status 1) (util/update-session seller-id :sales :orders))
  (let [values {:status status :updated_on (raw "(now())")}
        values (if (= status 1) (assoc values :auto_finalize (raw "(now() + interval '17 days')")) values)]
  (update orders
          (set-fields values)
          (where {:seller_id seller-id :id [in sales]}))))

;;use update instead of select... genius
(defn finalize [id user-id]
  (util/update-session user-id :orders :sales)
  (let [id (util/parse-int id)
        {:keys [hedge_fee seller_id listing_id hedged]} (update orders (set-fields {:status 3 :updated_on (raw "now()")}) (where {:id id :user_id user-id :status [not= 3]}))
        {amount :amount currency_id :currency_id} (update escrow (set-fields {:status "done" :updated_on (raw "now()")}) (where {:order_id id :from user-id :status "hold"}))
        amount (util/convert-price currency_id 1 amount)
        percent (if hedged hedge_fee (env :fee))
        fee_amount (* percent amount)
        audit {:amount (- amount fee_amount) :user_id seller_id :role "sale"}
        fee {:order_id id :role "order" :amount fee_amount}]
    (when (not (nil? listing_id))
      (transaction
        (insert fees (values fee))
        (insert audits (values audit))
        (update users (set-fields {:btc (raw (str "btc + " (- amount fee_amount)))}) (where {:id seller_id}))
        (update listings (set-fields {:sold (raw "sold + 1") :updated_on (raw "now()")}) (where {:id listing_id})))
      (util/update-session seller_id))))

(defn resolution [id user-id]
  (util/update-session user-id :orders :sales)
  (update orders
          (set-fields {:status 2 :updated_on (raw "now()")})
          (where {:user_id user-id :id (util/parse-int id)})))


;;cancel button does not work
;;make sure to separate logic here
;;so that catagories and things are updated as the sales are rejected.
(defn reject-sales [sales seller-id]
  (let [o (select orders
                  (where {:seller_id seller-id :status 0 :id [in sales]}))]
    (dorun (map #(cancel! %) o))))

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
