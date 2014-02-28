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
    (let [values {}
          values (if (= (:seller_id res) user-id) (assoc values :seller_accepted true) values)
          values (if (= (:user_id res) user-id) (assoc values :user_accepted true) values)]
      (if (or (and (:user_accepted res) (:seller_accepted values))
              (and (:seller_acceptd res) (:user_accepted values)))
          (do 
            (if (= (:action res) "extension")
              (update orders 
                      (set-fields {:auto_finalize (raw (str "(auto_finalize + interval '" (:value res)  " days')"))})
                      (where {:id (:order_id res)}))
              )
            (update resolutions
                  (set-fields values)
                  (where {:id id})))))))

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
                 :content content
                 :seller_id seller-id
                 :user_id buyer-id
                 :user_accepted (= user-id buyer-id)
                 :seller_accepted (= user-id seller-id)
                 :action (if (= "refund" "extension" action) action)
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
