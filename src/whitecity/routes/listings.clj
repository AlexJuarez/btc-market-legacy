(ns whitecity.routes.listings
  (:use
    [compojure.core :only [GET POST context defroutes]]
    [noir.util.route :only (wrap-restricted)]
    [whitecity.helpers.route])
  (:require [whitecity.views.layout :as layout]
            [noir.response :as resp]
            [noir.session :as session]
            [whitecity.models.listing :as listing]
            [whitecity.models.category :as category]
            [whitecity.models.bookmark :as bookmark]
            [whitecity.models.review :as review]
            [whitecity.models.region :as region]
            [whitecity.models.report :as report]
            [whitecity.models.image :as image]
            [clojure.string :as string]
            [whitecity.models.postage :as postage]
            [whitecity.models.currency :as currency]
            [whitecity.util :as util]))

(def per-page 10)

(defn listings-page [page]
  (let [listing-count (:listings (util/current-user))
        page (or (util/parse-int page) 1)
        pagemax (util/page-max listing-count per-page)]
  (layout/render "listings/index.html" (conj (set-info) {:page {:page page :max pagemax}
                                                         :postages (postage/all (user-id))
                                                         :listings (listing/all (user-id) page per-page)}))))

(defn listing-remove [id]
  (let [record (listing/remove! id (user-id))]
  (if (nil? record)
    (resp/redirect "/vendor/")
  (do
    (session/flash-put! :success "listing removed")
    (resp/redirect "/vendor/listings")))))
;;Check convert currency set this to a global constant

(defn walk-current [lis c]
  (loop [l lis]
    (if (or (empty? l) (= (second (last l)) (:parent c)))
      (conj l [(:name c) (:id c)])
      (recur (pop l)))))

(defn create-categories [categories]
  (loop [cats categories
         current []
         output []]
    (if (empty? cats)
      output
      (let [c (first cats)
            curr (if (empty? current) [[(:name c) (:id c)]] (walk-current current c))]
        (recur (rest cats) curr (conj output (assoc c :name (string/join " > " (map first curr)))))))))

(defn listing-edit [id]
  (let [listing (listing/get id)
        success (session/flash-get :success)]
    (layout/render "listings/create.html" (merge {:regions (region/all) :min-price (util/convert-currency 1 0.01)
                                                  :edit true :success success :id id
                                                  :images (image/get (user-id)) :listing listing
                                                  :categories (create-categories (category/all))
                                                  :currencies (currency/all)} (set-info) listing))))

(defn listing-save [{:keys [id image image_id] :as slug}]
  (println slug)
  (let [listing (listing/update! (assoc slug :image_id (parse-image image_id image)) id (user-id))]
    (layout/render "listings/create.html" (merge {:regions (region/all) :min-price (util/convert-currency 1 0.01)
                                                  :edit true :success "updated" :id id
                                                  :images (image/get (user-id))
                                                  :categories (create-categories (category/all))
                                                  :currencies (currency/all)} (set-info) listing))))

(defn listing-create
  "Listing creation page"
  ([]
   (layout/render "listings/create.html" (conj {:regions (region/all)
                                                :images (image/get (user-id))
                                                :categories (create-categories (category/all))
                                                :currencies (currency/all)} (set-info))))
  ([{:keys [image image_id] :as slug}]
   (let [listing (listing/add! (assoc slug :image_id (parse-image image_id image)) (user-id))]
     (if (empty? (:errors listing))
      (do
        (session/flash-put! :success "listing created")
        (resp/redirect (str "/vendor/listing/" (:id listing) "/edit")))
      (layout/render "listings/create.html" (merge {:regions (region/all)
                                                    :images (image/get (user-id))
                                                    :categories (create-categories (category/all))
                                                    :currencies (currency/all)} (set-info) listing))))))

(defn listing-bookmark [id]
  (if-let [bookmark (:errors (bookmark/add! id (user-id)))]
    (session/flash-put! :bookmark bookmark))
  (resp/redirect (str "/listing/" id)))

(defn listing-unbookmark [id referer]
  (bookmark/remove! id (user-id))
  (resp/redirect referer))

(defroutes listing-routes
  (wrap-restricted
   (context
    "/listing/:id" [id]
    (GET "/bookmark" [] (listing-bookmark id))
    (GET "/unbookmark" {{referer "referer"} :headers} (listing-unbookmark id referer))
    (GET "/report" {{referer "referer"} :headers} (report-add id (user-id) "listing" referer))
    (GET "/unreport" {{referer "referer"} :headers} (report-remove id (user-id) "listing" referer))))

  (wrap-restricted
   (context
    "/vendor/listings" []
    (GET "/" [page] (listings-page page))
    (GET "/create" [] (listing-create))
    (POST "/create" {params :params} (listing-create params))))

  (wrap-restricted
   (context
    "/vendor/listing/:id" [id]
    (GET "/edit" [] (listing-edit id))
    (GET "/remove" [] (listing-remove id))
    (POST "/edit" {params :params} (listing-save params)))))
