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
            [noir.response :as resp]
            [whitecity.util :as util]))

(def per-page 25)

(defn moderator-page [page]
  (let [page (or (util/parse-int page) 1)
        ;;orders (map #(assoc % :id (hashids/encrypt (:id %))) (order/moderate page per-page))
        ;;pagemax (util/page-max 10 per-page)
        ]
  (layout/render "moderate/index.html" (set-info))))

(def-restricted-routes moderator-routes
  (GET "/market/moderate" [page] (moderator-page page)))
