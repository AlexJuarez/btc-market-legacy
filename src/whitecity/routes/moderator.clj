(ns whitecity.routes.moderator
  (:use
    [compojure.core :only [GET POST]]
    [noir.util.route :only (def-restricted-routes)]
    [whitecity.helpers.route])
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.order :as order]
            [whitecity.models.review :as review]
            [whitecity.util.hashids :as hashids]
            [whitecity.models.resolution :as resolution]
            [whitecity.models.user :as user]
            [noir.response :as resp]
            [whitecity.util :as util]))

(def per-page 25)

(defn moderator-page [page]
  (let [page (or (util/parse-int page) 1)
        orders (map #(assoc % :id (hashids/encrypt (:id %))) (order/moderate page per-page))
        pagemax (util/page-max 10 per-page)
        ]
  (layout/render "moderate/index.html" (merge {:orders orders} (set-info)))))

(defn moderator-view [id]
  (let [id (hashids/decrypt id)
        order (-> (order/moderate-order id) encrypt-id convert-order-price)
        past-orders (order/count-past (:user_id order))
        seller (user/get (:seller_id order))
        buyer (user/get (:buyer_id order))
        resolutions (resolution/all id (user-id))]
    (layout/render "moderate/resolution.html" (merge {:resolutions resolutions :buyer buyer
                                                      :seller seller :past_orders past-orders} order (set-info)))))

(def-restricted-routes moderator-routes
  (GET "/market/moderate" [page] (moderator-page page))
  (GET "/market/moderate/:id" [id] (moderator-view id)))
