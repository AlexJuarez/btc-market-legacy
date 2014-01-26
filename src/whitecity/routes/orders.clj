(ns whitecity.routes.orders
  (:use compojure.core
        noir.util.route
        whitecity.helpers.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.order :as order]
            [whitecity.models.review :as review]
            [noir.response :as resp]
            [whitecity.util :as util]))

(defn orders-page 
  ([]
    (let [orders (order/all (user-id))
          orders (map #(let [autofinalize (java.sql.Timestamp. (+ 1468800000 (.getTime (:created_on %))))
                             res (> 432000000 (- (.getTime autofinalize) (.getTime (java.util.Date.))))] 
                           (assoc % :auto_finalize autofinalize :resolve res)) orders)
          pending-review (filter #(= (:status %) 3) orders)
          orders (filter #(< (:status %) 3) orders)]
       (layout/render "orders/index.html" (merge {:errors {} :orders orders :pending-review pending-review :user-id (user-id)} (set-info)))))
  ([{:keys [rating shipped content] :as slug}]
   (let [prep (map #(let [id (key %) value (val %)] {:order_id id :rating value :shipped (shipped id) :content (content id)}) rating)
         order-ids (map #(util/parse-int (key %)) rating)
         reviews (review/add! prep (user-id) order-ids)]
    (resp/redirect "/market/orders"))))
   
(defn order-finalize [id]
  (order/finalize id (user-id))
  (resp/redirect "/market/orders"))

(defn order-resolve [id])

(def-restricted-routes order-routes
    (GET "/market/orders" [] (orders-page))
    (POST "/market/orders" {params :params} (orders-page params))
    (GET "/market/order/:id/resolve" [id] (order-resolve id))
    (GET "/market/order/:id/finalize" [id] (order-finalize id)))
