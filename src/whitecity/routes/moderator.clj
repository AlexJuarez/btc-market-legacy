(ns whitecity.routes.moderator
  (:use
    [compojure.core :only [GET POST]]
    [noir.util.route :only (def-restricted-routes)]
    [whitecity.helpers.route])
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.order :as order]
            [whitecity.models.review :as review]
            [whitecity.util.hashids :as hashids]
            [whitecity.models.moderate :as moderate]
            [whitecity.models.resolution :as resolution]
            [whitecity.models.feedback :as feedback]
            [whitecity.models.user :as user]
            [noir.response :as resp]
            [whitecity.util :as util]))

(def per-page 25)

(defn encrypt-ticket-ids [tickets]
  (map #(assoc % :id (hashids/encrypt-ticket-id (:id %))) tickets))

(defn moderator-page [page]
  (let [page (or (util/parse-int page) 1)
        orders (-> (order/moderate page per-page) encrypt-ids)
        pagemax (util/page-max 10 per-page)
        support (encrypt-ticket-ids (feedback/all))]
  (layout/render "moderate/index.html" (merge {:orders orders
                                               :tickets support
                                               } (set-info)))))

(defn support-view [id]
  (let [id (hashids/decrypt-ticket-id id)
        ticket (feedback/get id)]
    (layout/render "moderate/support.html" (merge {:ticket ticket} (set-info)))
    ))

(defn est [resolutions total]
  (map #(assoc % :est (* (/ (:percent %) 100) total))
       resolutions))

(defn moderator-view [id & errors]
  (let [id (hashids/decrypt id)
        order (-> (order/moderate-order id) encrypt-id convert-order-price)
        past-orders (order/count-past (:user_id order))
        seller (user/get (:seller_id order))
        seller-resolutions (-> (order/past-resolutions (:seller_id order)) encrypt-ids)
        buyer (user/get (:user_id order))
        buyer-resolutions (-> (order/past-resolutions (:user_id order)) encrypt-ids)
        resolutions (resolution/all id)
        modresolutions (moderate/all id)]
    (layout/render "moderate/resolution.html" (merge order {:resolutions (estimate-refund resolutions order)
                                                            :modresolutions (est modresolutions (:total order))
                                                            :buyer buyer
                                                            :errors errors
                                                            :seller-rating (int (* (/ (:rating seller) 5) 100))
                                                            :buyer-resolutions buyer-resolutions
                                                            :seller-resolutions seller-resolutions
                                                            :seller seller :past_orders past-orders} (set-info)))))

(defn moderator-add-resolution [raw_id slug]
  (let [id (hashids/decrypt raw_id)
        res (moderate/add! id slug (user-id))]
    (moderator-view raw_id res)))

(def-restricted-routes moderator-routes
  (GET "/moderate" [page] (moderator-page page))
  (GET "/moderate/:id" [id] (moderator-view id))
  (GET "/moderate/support/:id" [id] (support-view id))
  (POST "/moderate/:id" {params :params} (moderator-add-resolution (:id params) params)))
