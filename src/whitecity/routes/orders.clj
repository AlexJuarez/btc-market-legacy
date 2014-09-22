(ns whitecity.routes.orders
  (:use
    [compojure.core :only [GET POST context defroutes]]
    [noir.util.route :only (def-restricted-routes wrap-restricted)]
    [whitecity.helpers.route])
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
                             res (and (not (nil? autofinalize)) (> 432000000 (- (.getTime autofinalize) (.getTime (java.util.Date.)))))
                             arbitration (and (= (:status %) 2) (<= (.getTime autofinalize) (.getTime (java.util.Date.))))];;TODO: review resolution stuff
                           (assoc % :resolve res :arbitration arbitration :id (hashids/encrypt (:id %)))) orders)
          pending-review (filter #(and (not (:reviewed %)) (:finalized %)) orders)
          orders (filter #(< (:status %) 3) orders)]
       (layout/render "orders/index.html" (merge {:errors {} :orders orders :pending-review pending-review :user-id (user-id)} (set-info)))))
  ([{:keys [rating shipped content] :as slug}]
   (let [prep (map #(let [id (key %) value (val %)] {:order_id (hashids/decrypt id) :rating value :shipped (shipped id) :content (content id)}) rating)
         order-ids (map #(util/parse-int (hashids/decrypt (key %))) rating)
         reviews (review/add! prep (user-id) order-ids)]
    (resp/redirect "/orders"))))

(defn order-finalize [id]
  (let [id (hashids/decrypt id)]
    (order/finalize id (user-id))
    (resp/redirect "/orders")))

(defn estimate-refund [resolutions {:keys [total]}]
  (map #(if (= (:action %) "refund")
            (assoc % :est (* (/ (:value %) 100) total))
          %
         ) resolutions))

(defn order-cancel [id]
  (let [id (hashids/decrypt id)]
    (order/cancel! id (user-id))
    (resp/redirect (str "/orders"))))

(defn order-view
  ([id]
    (let [id (hashids/decrypt id)
          order (-> (order/get-order id (user-id)) encrypt-id convert-order-price)
          arbitration (and (= (:status order) 2) (<= (.getTime (:auto_finalize order)) (.getTime (java.util.Date.))))
          resolutions (estimate-refund (resolution/all id (user-id)) order)]
      (layout/render "orders/resolution.html" (merge {:errors {} :action "extension" :arbitration arbitration :resolutions resolutions :order order} order (set-info)))))
  ([slug post]
    (let [id (hashids/decrypt (:id slug))
          res (resolution/add! slug id (user-id))
          order (-> (order/get-order id (user-id)) encrypt-id convert-order-price)
          resolutions (estimate-refund (resolution/all id (user-id)) order)]
      (layout/render "orders/resolution.html" (merge {:errors {} :resolutions resolutions} slug res order (set-info))))))

(defn order-resolve [hashid]
  (let [id (hashids/decrypt hashid)]
    (order/resolution id (user-id))
    (resp/redirect (str "/order/" hashid))))

(defroutes order-routes
  (wrap-restricted
   (context
    "/orders" []
    (GET "/" [] (orders-page))
    (POST "/" {params :params} (orders-page params))))
  (wrap-restricted
   (context
    "/order/:id" [id]
    (GET "/resolve" [id] (order-resolve id))
    (GET "/cancel" [id] (order-cancel id))
    (GET "/" [id] (order-view id))
    (POST "/" {params :params} (order-view params true))
    (GET "/finalize" [id] (order-finalize id)))))
