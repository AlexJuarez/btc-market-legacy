(ns whitecity.models.resolution
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [whitecity.util :as util]))

(defn all [order-id user-id]
  (select resolutions
          (where (and {:order_id (util/parse-int order-id)} (or {:user_id user-id} {:seller_id user-id})))))

(defn store! [resolution]
  (insert resolutions (values resolution)))

(defn prep [{:keys [action extension refund content]} order-id user-id]
  (let [order (first (select orders (where {:id order-id})))
        seller-id (:seller_id order)
        buyer-id (:user_id order)]
    (if (or (= user-id seller-id) (= user-id buyer-id))
      (-> {:content content
           :seller_id seller-id
           :user_id buyer-id
           :order_id order-id}
        (if (= action "refund") (assoc :refund (util/parse-int refund)) (assoc :extension (util/parse-int extension)))))))

(defn add! [slug order-id user-id]
  (let [resolution (prep slug)
        check (if (nil? (:refund resolution)) (v/resolution-refund-validator resolution) (v/resolution-extension-validator resolution))]
        (if (empty? check)
          (store! resolution)
          {:errors check})))

