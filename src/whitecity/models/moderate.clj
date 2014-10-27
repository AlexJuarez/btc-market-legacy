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

(defn all [order-id]
  (select modresolutions
          (with users
                (fields :alias))
          (where {:order_id order-id})))

