(ns whitecity.routes.sales
  (:use compojure.core
        noir.util.route
        whitecity.helpers.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.order :as order]
            [whitecity.util.hashids :as hashids]
            [whitecity.models.order :as order]
            [whitecity.models.resolution :as resolution]
            [noir.response :as resp]
            [whitecity.util :as util]))

(defn sales-new 
  []
  (let [sales (encrypt-ids (order/sold 0 (user-id)))]
     (layout/render "sales/new.html" (merge {:status 0 :sales sales} (set-info)))))

(defn sales-overview 
  []
  (let [sales (encrypt-ids (order/sold (user-id)))]
     (layout/render "sales/overview.html" (merge {:status 1 :sales sales} (set-info)))))

(defn sales-shipped 
  []
  (let [orders (encrypt-ids (order/sold 1 (user-id)))]
     (layout/render "sales/shipped.html" (merge {:status 2 :sales orders} (set-info)))))

(defn sales-disputed 
  []
  (let [sales (encrypt-ids (order/sold 2 (user-id)))]
     (layout/render "sales/disputed.html" (merge {:status 3 :sales sales} (set-info)))))

(defn sales-finailized 
  []
  (let [sales (encrypt-ids (order/sold 3 (user-id)))]
     (layout/render "sales/finailized.html" (merge {:status 4 :sales sales} (set-info)))))

(defn sales-view 
  ([hashid]
    (let [id (hashids/decrypt hashid)
          order (-> (order/get-sale id (user-id)) encrypt-id)
          resolutions (resolution/all-sales id (user-id))]
      (layout/render "sales/resolution.html" (merge {:errors {} :action "extension" :resolutions resolutions} order (set-info)))))
  ([slug post]
    (let [id (hashids/decrypt (:id slug))
          res (resolution/add! slug id (user-id))
          order (-> (order/get-sale id (user-id)) encrypt-id)
          resolutions (resolution/all-sales id (user-id))]
        (layout/render "sales/resolution.html" (merge {:errors {} :resolutions resolutions} res slug order (set-info))))))

(defn sales-page
  ([] (sales-overview))
  ([{:keys [submit check] :as slug}]
   (let [sales (map #(-> % name hashids/decrypt util/parse-int) (keys check))]
     (if (= submit "accept")
       (do (order/update-sales sales (user-id) 1) (resp/redirect "/market/sales"))
       (do (order/reject-sales sales (user-id)) (resp/redirect "/market/sales"))))))

(def-restricted-routes sales-routes
    (GET "/market/sales" [] (sales-overview))
    (GET "/market/sale/:id" [id] (sales-view id))
    (POST "/market/sale/:id" {params :params} (sales-view params true))
    (GET "/market/sales/new" [] (sales-new))
    (GET "/market/sales/shipped" [] (sales-shipped))
    (GET "/market/sales/resolutions" [] (sales-disputed))
    (GET "/market/sales/past" [] (sales-finailized))
    (POST "/market/sales/new" {params :params} (sales-page params)))
