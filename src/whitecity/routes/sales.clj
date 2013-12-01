(ns whitecity.routes.sales
  (:use compojure.core
        noir.util.route
        whitecity.helpers.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [whitecity.models.message :as message]
            [whitecity.models.listing :as listing]
            [whitecity.models.image :as image]
            [whitecity.models.postage :as postage]
            [whitecity.models.order :as order]
            [noir.response :as resp]
            [clj-time.core :as cljtime]
            [clj-time.coerce :as tc]
            [clojure.string :as string]
            [noir.session :as session]
            [noir.io :as io]
            [whitecity.util :as util]))

(defn sales-new 
  []
  (let [sales (order/sold 0 (user-id))]
     (layout/render "sales/new.html" (merge {:sales sales} (set-info)))))

(defn sales-overview 
  []
  (let [sales (order/sold 0 (user-id))]
     (layout/render "sales/new.html" (merge {:sales sales} (set-info)))))

(defn sales-shipped 
  []
  (let [sales (order/sold 1 (user-id))]
     (layout/render "sales/shipped.html" (merge {:sales sales} (set-info)))))

(defn sales-disputed 
  []
  (let [sales (order/sold 2 (user-id))]
     (layout/render "sales/disputed.html" (merge {:sales sales} (set-info)))))

(defn sales-finailized 
  []
  (let [sales (order/sold 3 (user-id))]
     (layout/render "sales/finailized.html" (merge {:sales sales} (set-info)))))

(defn sales-page
  ([] (sales-overview))
  ([{:keys [submit check] :as slug}]
   (let [sales (map util/parse-int (keys check))]
     (if (= submit "accept")
       (do (order/update-sales sales (user-id) 1) (resp/redirect "/market/sales"))
       (do (order/reject-sales sales (user-id)))))))

(def-restricted-routes sales-routes
    (GET "/market/sales" [] (sales-overview))
    (GET "/market/sales/new" [] (sales-new))
    (GET "/market/sales/shipped" [] (sales-shipped))
    (GET "/market/sales/resolutions" [] (sales-disputed))
    (GET "/market/sales/past" [] (sales-finailized))
    (POST "/market/sales/new" {params :params} (sales-page params)))
