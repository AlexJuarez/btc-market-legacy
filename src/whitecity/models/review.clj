(ns whitecity.models.review
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [whitecity.util :as util]))

(defn prep [{:keys [order_id rating content shipped]} user-id]
  {:order_id (util/parse-int order_id)
   :rating (max 0 (min 5 (util/parse-int rating)))
   :content content
   :shipped (= "true" shipped)
   :user_id user-id})

(defn store! [reviews user-id order-ids]
    (let [os (select order (where (and [in order-ids] {:user_id user-id :reviewed false})))
          listing-ids (map #(:listing_id %) os)
          reviews (map #(prep % user-id) reviews)]
    ))

(defn add! [reviews user-id order-ids]
  (store! reviews user-id order-ids))
