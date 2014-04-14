(ns whitecity.models.resolution
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [whitecity.util :as util]))

(def actions #{"refund" "extension"})

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
                    (fields :seller_id :user_id)
                    (where {:id id})))] ;;added a flag to see if the resolution was used
    (if-not (:applied res)
      (let [values {:applied true}
            values (if (= (:seller_id res) user-id) (assoc values :seller_accepted true) values)
            values (if (= (:user_id res) user-id) (assoc values :user_accepted true) values)]
        (if (or 
              (and (:user_accepted values) (:seller_accepted res))
              (and (:user_accepted res) (:seller_accepted values))) ;;check to see if everyone wants this resolution
          (if (= (:action res) "extension")
            (transaction
              (update resolutions
                      (set-fields values)
                      (where {:id id}))
              (update orders 
                      (set-fields {:auto_finalize (raw (str "(auto_finalize + interval '" (:value res)  " days')"))})
                      (where {:id (:order_id res)})))
            (let [user_id (:user_id res)
                  seller_id (:seller_id res)
                  {amount :amount currency_id :currency_id} (first (select escrow (where {:order_id id :from user_id :status "hold"})))
                  user-amount (* (util/convert-price currency_id 1 amount) (/ (:value res) 100))
                  seller-amount (- amount user-amount) 
                  user-audit {:amount amount :user user_id :role "refund"}
                  seller-audit {:amount amount :user seller_id :role "refund"}]
              (trasaction
                (update resolutions
                        (set-fields values)
                        (where {:id id}))
                (insert audits (values user-audit))
                (insert audits (values seller-audit))
                (update users (set-fields {:btc (raw (str "btc + " user-amount))}) (where {:id user_id}))
                (update users (set-fields {:btc (raw (str "btc + " seller-amount))}) (where {:id seller_id}))
                (update escrow (set-fields {:status "done" :updated_on (raw "now()")}) (where {:order_id (:order_id res)}))
                (update orders (set-fields {:status 3 :updated_on (raw "now()")})
                        (where {:user_id user-id :id (util/parse-int id)}))))))))))

(defn store! [resolution]
  (insert resolutions (values resolution)))

(defn prep [{:keys [action extension refund content]} order-id user-id]
  "prepares content for the resolutions table, 
  takes in a map with an action, extension, refund and message"
  (let [order-id (util/parse-int order-id)
        order (first (select orders (where {:id order-id})))
        seller-id (:seller_id order)
        buyer-id (:user_id order)]
      (let [res {:from user-id
                 :applied false
                 :content content
                 :seller_id seller-id
                 :user_id buyer-id
                 :user_accepted (= user-id buyer-id)
                 :seller_accepted (= user-id seller-id)
                 :action (if (contains? actions action) action)
                 :value (if (= action "refund") (util/parse-int refund) (util/parse-int extension))
                 :order_id order-id}]
        (if (nil? (:value res)) (dissoc res :value) res))))

(defn add! [slug order-id user-id]
  (let [resolution (prep slug order-id user-id)
        check (if (= "refund" (:action resolution))
                (v/resolution-refund-validator resolution) (v/resolution-extension-validator resolution))]
        (if (empty? check)
          (store! resolution)
          {:errors check})))
