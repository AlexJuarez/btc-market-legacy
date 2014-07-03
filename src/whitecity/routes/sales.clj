(ns whitecity.routes.sales
  (:use
    [compojure.core :only [GET POST]]
    [environ.core :only [env]]
    [noir.util.route :only (def-restricted-routes)]
    [whitecity.helpers.route])
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.order :as order]
            [whitecity.util.hashids :as hashids]
            [whitecity.models.order :as order]
            [whitecity.models.resolution :as resolution]
            [noir.response :as resp]
            [whitecity.util :as util]))

(def sales-per-page 50)

(defn get-sales [k]
  ((util/session! :sales (order/count-sales (user-id))) k))

(defn calculate-amount [sales]
  (map #(let [price (util/convert-currency (:currency_id %) (:price %))
              postage-price (util/convert-currency (:postage_currency %) (:postage_price %))
              percent (if (:hedged %) (:hedge_fee %) (env :fee))
              amount (+ (* price (:quantity %)) postage-price)
              fee (* amount percent)]
          (assoc % :amount amount :fee fee)) sales))

(defn sales [template url status page]
  (let [page (or (util/parse-int page) 1)
        state ([:new :ship :resolution :finalize] status)
        pagemax (util/page-max (get-sales state) sales-per-page)
        sales (calculate-amount (encrypt-ids (order/sold status (user-id) page sales-per-page)))]
     (layout/render template (merge {:sales sales :page {:page page :max pagemax :url url}} (set-info)))))

(defn sales-new
  [page]
  (sales "sales/new.html" "/market/sales/new" 0 page))

(defn sales-shipped
  [page]
  (sales "sales/shipped.html" "/market/sales/shipped" 1 page))

(defn sales-disputed
  [page]
  (sales "sales/disputed.html" "/market/sales/resolutions" 2 page))

(defn sales-finailized
  [page]
  (sales "sales/finailized.html" "/market/sales/past" 3 page))

(defn sales-overview
  [page]
  (let [page (or (util/parse-int page) 1)
        pagemax (util/page-max (get-sales :total) sales-per-page)
        sales (encrypt-ids (order/sold (user-id) page sales-per-page))]
     (layout/render "sales/overview.html" (merge {:sales sales :page {:page page :max pagemax :url "/market/sales"}} (set-info)))))

(defn sales-view
  ([hashid]
    (let [id (hashids/decrypt hashid)
          order (-> (order/get-sale id (user-id)) encrypt-id convert-order-price)
          resolutions (resolution/all-sales id (user-id))]
      (layout/render "sales/resolution.html" (merge {:errors {} :action "extension" :resolutions resolutions} order (set-info)))))
  ([slug post]
    (let [id (hashids/decrypt (:id slug))
          res (resolution/add! slug id (user-id))
          order (-> (order/get-sale id (user-id)) encrypt-id convert-order-price)
          resolutions (resolution/all-sales id (user-id))]
        (layout/render "sales/resolution.html" (merge {:errors {} :resolutions resolutions} res slug order (set-info))))))

(defn sales-page
  ([] (sales-overview 1))
  ([{:keys [submit check] :as slug}]
   (let [sales (map #(-> % name hashids/decrypt util/parse-int) (keys check))]
     (if (= submit "accept")
       (do (order/update-sales sales (user-id) 1) (resp/redirect "/market/sales"))
       (do (order/reject-sales sales (user-id)) (resp/redirect "/market/sales"))))))

(def-restricted-routes sales-routes
    (GET "/market/sales" {{page :page} :params} (sales-overview page))
    (GET "/market/sales/new" {{page :page} :params} (sales-new page))
    (GET "/market/sales/shipped" {{page :page} :params} (sales-shipped page))
    (GET "/market/sales/resolutions" {{page :page} :params} (sales-disputed page))
    (GET "/market/sales/past" {{page :page} :params} (sales-finailized page))
    (POST "/market/sales/new" {params :params} (sales-page params))
    (GET "/market/sale/:id" [id] (sales-view id))
    (POST "/market/sale/:id" {params :params} (sales-view params true)))
