(ns whitecity.routes.orders
  (:use compojure.core
        noir.util.route
        whitecity.helpers.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.order :as order]
            [whitecity.models.review :as review]
            [whitecity.util.hashids :as hashids]
            [whitecity.models.resolution :as resolution]
            [noir.response :as resp]
            [whitecity.util :as util]))

(defn orders-page 
  ([]
    (let [orders (order/all (user-id))
          orders (map #(let [autofinalize (:auto_finalize %)
                             res (and (not (nil? autofinalize)) (< 432000000 (- (.getTime autofinalize) (.getTime (java.util.Date.)))))];;TODO: switch symbol back
                           (assoc % :resolve res :id (hashids/encrypt (:id %)))) orders)
          pending-review (filter #(= (:status %) 3) orders)
          orders (filter #(< (:status %) 3) orders)]
       (layout/render "orders/index.html" (merge {:errors {} :orders orders :pending-review pending-review :user-id (user-id)} (set-info)))))
  ([{:keys [rating shipped content] :as slug}]
   (let [prep (map #(let [id (key %) value (val %)] {:order_id id :rating value :shipped (shipped id) :content (content id)}) rating)
         order-ids (map #(util/parse-int (key %)) rating)
         reviews (review/add! prep (user-id) order-ids)]
    (resp/redirect "/market/orders"))))
   
(defn order-finalize [id]
  (let [id (hashids/decrypt id)]
    (order/finalize id (user-id))
    (resp/redirect "/market/orders")))

(defn order-view 
  ([id]
    (let [id (hashids/decrypt id)
          order (encrypt-id (order/get-order id (user-id)))
          resolutions (resolution/all id (user-id))]
      (layout/render "orders/resolution.html" (merge {:errors {} :action "extension" :resolutions resolutions} order (set-info)))))
  ([slug post]
    (let [id (hashids/decrypt (:id slug))
          res (resolution/add! slug id (user-id))
          order (encrypt-id (order/get-order id (user-id)))
          resolutions (resolution/all id (user-id))]
      (layout/render "orders/resolution.html" (merge {:errors {} :resolutions resolutions} slug res order (set-info))))))
    
(defn order-resolve [hashid]
  (let [id (hashids/decrypt hashid)]
  (order/resolution id (user-id))
  (resp/redirect (str "/market/order/" hashid))))

(def-restricted-routes order-routes
    (GET "/market/orders" [] (orders-page))
    (POST "/market/orders" {params :params} (orders-page params))
    (GET "/market/order/:id/resolve" [id] (order-resolve id))
    (GET "/market/order/:id" [id] (order-view id))
    (POST "/market/order/:id" {params :params} (order-view params true))
    (GET "/market/order/:id/finalize" [id] (order-finalize id)))
