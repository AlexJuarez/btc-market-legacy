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
      (store! resolution)
      {:errors check})))

(defn all [order-id]
  (select modresolutions (where {:order_id order-id})))
