(ns whitecity.routes.sales
  (:use
    [compojure.core :only [GET POST defroutes context]]
    [environ.core :only [env]]
    [noir.util.route :only (wrap-restricted)]
    [whitecity.helpers.route])
  (:require [whitecity.views.layout :as layout]
            [ring.util.response :as r :refer [content-type response]]
            [whitecity.models.order :as order]
            [whitecity.util.hashids :as hashids]
            [whitecity.models.order :as order]
            [whitecity.models.resolution :as resolution]
            [noir.response :as resp]
            [clojure.string :as string]
            [whitecity.util :as util]))

(def sales-per-page 100)

(defn get-sales [k]
  ((util/session! :sales (order/count-sales (user-id))) k))

(defn arbitration [sales]
  (map #(let [arbitration (and (= (:status %) 2)
                               (<= (.getTime (:auto_finalize %)) (.getTime (java.util.Date.))))]
          (assoc % :arbitration arbitration)) sales))

(defn calculate-amount [sales]
  (map #(let [price (util/convert-currency (:currency_id %) (:price %))
              postage-price (util/convert-currency (:postage_currency %) (:postage_price %))
              percent (if (:hedged %) (:hedge_fee %) (env :fee))
              amount (+ (* price (:quantity %)) postage-price)
              fee (* amount percent)]
          (assoc % :arbitration arbitration :amount amount :fee fee)) sales))

(defn sales [template url status page]
  (let [page (or (util/parse-int page) 1)
        state ([:new :ship :resolution :finalize] status)
        pagemax (util/page-max (get-sales state) sales-per-page)
        sales (-> (order/sold status (user-id) page sales-per-page) encrypt-ids calculate-amount arbitration)]
     (layout/render template (merge {:sales sales :page page :page-info {:page page :max pagemax :url url}} (set-info)))))

(defn estimate-refund [resolutions {:keys [total]}]
  (map #(if (= (:action %) "refund")
            (assoc % :est (* (/ (- 100 (:value %)) 100) total))
          %
         ) resolutions))

(defn sales-new
  [page]
  (sales "sales/new.html" "/vendor/sales/new" 0 page))

(defn sales-shipped
  [page]
  (sales "sales/shipped.html" "/vendor/sales/shipped" 1 page))

(defn sales-disputed
  [page]
  (sales "sales/disputed.html" "/vendor/sales/resolutions" 2 page))

(defn sales-finailized
  [page]
  (sales "sales/finailized.html" "/vendor/sales/past" 3 page))

(defn sales-overview
  [page]
  (let [page (or (util/parse-int page) 1)
        pagemax (util/page-max (get-sales :total) sales-per-page)
        sales (-> (order/sold (user-id) page sales-per-page) encrypt-ids arbitration)]
     (layout/render "sales/overview.html" (merge {:sales sales :page {:page page :max pagemax :url "/vendor/sales"}} (set-info)))))

(defn sales-view
  ([hashid]
    (let [id (hashids/decrypt hashid)
          order (-> (order/get-sale id (user-id)) encrypt-id convert-order-price)
          arbitration (and (= (:status order) 2) (<= (.getTime (:auto_finalize order)) (.getTime (java.util.Date.))))
          resolutions (estimate-refund (resolution/all-sales id (user-id)) order)]
      (layout/render "sales/resolution.html" (merge {:errors {} :arbitration arbitration
                                                     :action "extension" :resolutions resolutions} order (set-info)))))
  ([slug post]
    (let [id (hashids/decrypt (:id slug))
          res (resolution/add! slug id (user-id))
          order (-> (order/get-sale id (user-id)) encrypt-id convert-order-price)
          resolutions (estimate-refund (resolution/all-sales id (user-id)) order)]
        (layout/render "sales/resolution.html" (merge {:errors {} :resolutions resolutions} res slug order (set-info))))))

(defn sales-page
  ([] (sales-overview 1))
  ([{:keys [submit check] :as slug}]
   (let [sales (map #(-> % name hashids/decrypt util/parse-int) (keys check))]
     (if (= submit "accept")
       (do (order/update-sales sales (user-id) 1) (resp/redirect "/vendor/sales"))
       (do (order/reject-sales sales (user-id)) (resp/redirect "/vendor/sales"))))))

(defn sales-download [status page]
  (let [page (or (util/parse-int page) 1)
        state ([:new :ship :resolution :finalize] status)
        pagemax (util/page-max (get-sales state) sales-per-page)
        sales (-> (order/sold status (user-id) page sales-per-page) encrypt-ids calculate-amount arbitration)
        saleview (string/join "\n" (map #(str "\"" (:id %) "\",\"" (string/replace (:title %) #"[\"]" "\"\"") "\",\""
                                              (string/replace (:postage_title %) #"[\"]" "\"\"") "\",\""
                                              (:quantity %) "\",\""
                                              (:alias %) "\",\""
                                              (:amount %) "\",\""
                                              (:fee %) "\",\""
                                              (string/replace (:address %) #"[\"]" "\"\"") "\"") sales))]
    (-> (response saleview)
        (content-type "text/plain")
        (r/header "Content-Disposition" (str "attachment;filename=" (util/format-time (java.util.Date. ) "MM-dd-yyyy") "-sales-" (name state) "-" page ".csv")))))

(defroutes sales-routes
  (wrap-restricted
   (context
    "/vendor/sales" []
    (GET "/" {{page :page} :params} (sales-overview page))
    (GET "/new" {{page :page} :params} (sales-new page))
    (GET "/new/download" {{page :page} :params} (sales-download 0 page))
    (GET "/shipped" {{page :page} :params} (sales-shipped page))
    (GET "/shipped/download" {{page :page} :params} (sales-download 1 page))
    (GET "/resolutions" {{page :page} :params} (sales-disputed page))
    (GET "/past" {{page :page} :params} (sales-finailized page))
    (POST "/new" {params :params} (sales-page params))
    ))
  (wrap-restricted
   (context
    "/vendor/sale/:id" [id]
    (GET "/" [id] (sales-view id))
    (POST "/" {params :params} (sales-view params true)))))
