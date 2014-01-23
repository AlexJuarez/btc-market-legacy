(ns whitecity.models.review
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [whitecity.util :as util]))

(defn get [listing-id page]
  (select reviews
          (where {:listing_id (util/parse-int listing-id)})))

(defn for-user [user-id]
  (select reviews
          (where {:seller_id (util/parse-int user-id)})))

(defn prep [{:keys [order_id rating content shipped]} user-id order-info]
  (if-let [order-info (order-info (util/parse-int order_id))] 
    {:order_id (util/parse-int order_id)
     :published true
     :seller_id (:seller_id order-info)
     :listing_id (:listing_id order-info)
     :rating (max 0 (min 5 (util/parse-int rating)))
     :content content
     :shipped (= "true" shipped)
     :user_id user-id}))

(defn store! [{:keys [order_id seller_id listing_id rating] :as review}]
      (transaction
        (update orders
                (set-fields {:reviewed true})
                (where {:id order_id}))
        (update listings
                (set-fields {:reviews (raw "reviews + 1")})
                (where {:id listing_id}))
        (update users
                (set-fields {:transactions (raw "transactions + 1") :rating (raw (str "rating*transactions/(transactions+1) + (" rating ")/(transactions+1)"))})
                (where {:id seller_id}))
        (insert reviews
                (values review))))

(defn add! [data user-id order-ids]
  (let [os (select orders (where {:id [in order-ids] :user_id user-id :reviewed false}))
        order-info (apply merge (map #(assoc {} (:id %) %) os))
        prepped (map #(prep % user-id order-info) data)]
  (dorun (pmap store! prepped))))
