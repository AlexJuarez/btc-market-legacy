(ns whitecity.models.resolution
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [whitecity.util :as util]))

(defn all [order-id user-id]
  (select resolutions
          (with sellers
                (fields :alias))
          (where {:order_id (util/parse-int order-id) :user_id user-id})
          (order :created_on :ASC)))

(defn all-sales [order-id seller-id]
  (select resolutions
          (with users
                (fields :alias))
          (where {:order_id (util/parse-int order-id) :seller_id seller-id})
          (order :created_on :ASC)))

(defn accept [id user-id]
  (let [id (util/parse-int id)
        res (first (select resolutions
                    (where {:id id})))]
    (if (and (:user_accepted res) (:seller_accepted res))
      (let [values {}
            values (if (= (:seller_id res) user-id) (assoc values :seller_accepted true) values)
            values (if (= (:user_id res) user-id) (assoc values :user_accepted true) values)]
        (if (not (empty? values))
          (update resolutions
                  (set-fields values)
                  (where {:id id})))))

(defn store! [resolution]
  (insert resolutions (values resolution)))

(defn prep [{:keys [action extension refund content]} order-id user-id]
  (let [order-id (util/parse-int order-id)
        order (first (select orders (where {:id order-id})))
        seller-id (:seller_id order)
        buyer-id (:user_id order)]
    (if (or (= user-id seller-id) (= user-id buyer-id))
      (let [res {:from user-id
                 :content content
                 :seller_id seller-id
                 :user_id buyer-id
                 :user_accepted (= user-id buyer-id)
                 :seller_accepted (= user-id seller-id)
                 :order_id order-id}
            res (if (= action "refund") (assoc res :refund (util/parse-int refund)) (assoc res :extension (util/parse-int extension)))]
        res))))

(defn add! [slug order-id user-id]
  (let [resolution (prep slug order-id user-id)
        check (if (= "refund" (:action slug))
                (v/resolution-refund-validator resolution) (v/resolution-extension-validator resolution))]
        (if (empty? check)
          (store! resolution)
          {:errors check})))
