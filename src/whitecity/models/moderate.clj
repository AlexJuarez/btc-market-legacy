(ns whitecity.models.moderate
   (:use [korma.db :only (transaction)]
         [korma.core]
         [whitecity.db])
  (:require
        [whitecity.validator :as v]
        [whitecity.util :as util]))

(defn prep [order_id slug user-id]
  (let [order (first (select orders
                 (with sellers (fields :login :alias))
                 (where {:id order_id})))]
    {:user_id user-id
     :buyer_id (:user_id order)
     :seller_id (:seller_id order)
     :order_id (:id order)
     :percent (util/parse-int (:percent slug))
     :content (:content slug)}
    ))

(defn store! [resolution]
  (insert modresolutions (values resolution)))

(defn add! [id slug user-id]
  (let [resolution (prep id slug user-id)
        check (v/modresolution-validator resolution)]
    (if (empty? check)
      (do (store! resolution) nil)
      check)))

(defn get-vote [res user-id]
  (let [res (util/parse-int res)]
    (first
     (select modvotes (where {:modresolution_id res :user_id user-id})))))

(defn voted? [res user-id]
  (not (nil? (get-vote res user-id))))

(defn vote! [res user-id]
  (let [res (util/parse-int res)]
    (transaction
     (update modresolutions
             (set-fields {:votes (raw "votes + 1")})
             (where {:id res}))
     (insert modvotes (values {:modresolution_id res :user_id user-id})))))

(defn remove-vote! [res user-id]
  (when (voted? res user-id)
    (let [res (util/parse-int res)]
      (transaction
       (update modresolutions
               (set-fields {:votes (raw "votes - 1")})
               (where {:id res}))
       (delete modvotes (values {:modresolution_id res :user_id user-id}))))))

(defn refund [order_id res user-id]
  (let [res (util/parse-int res)
        {amount :btc_amount} (update escrow (set-fields {:status "done" :updated_on (raw "now()")}) (where {:order_id order_id :status "hold"}))
        {user_id :user_id seller_id :seller_id} (first (select orders (where {:id order_id})))
        {percent :percent} (first (select modresolutions (where {:id res})))
        user-amount (* amount (/ percent 100))
        seller-amount (- amount user-amount)
        user-audit {:amount user-amount :user_id user_id :role "refund"}
        seller-audit {:amount seller-amount :user_id seller_id :role "refund"}]
    (util/update-session user_id :orders :sales)
    (util/update-session seller_id :orders :sales)
    (if amount
      (transaction
        (update resolutions
                (set-fields res)
                (where {:id res}))
        (insert audits (values [user-audit seller-audit]))
        (update users (set-fields {:btc (raw (str "btc + " user-amount))}) (where {:id user_id}))
        (update users (set-fields {:btc (raw (str "btc + " seller-amount))}) (where {:id seller_id}))
        (insert order-audit (values {:status 5 :order_id order_id :user_id user_id}))
        (update orders (set-fields {:status 5 :finalized true :updated_on (raw "now()")})
                (where {:user_id user_id :finalized false :id order_id}))))))

(defn all [order-id]
  (select modresolutions
          (with users
                (fields :alias))
          (where {:order_id order-id})))
