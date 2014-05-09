(ns whitecity.routes.listings
  (:use
    [compojure.core :only [GET POST]]
    [noir.util.route :only (def-restricted-routes)]
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
            [whitecity.models.postage :as postage]
            [whitecity.models.currency :as currency]
            [whitecity.util :as util]))

(def per-page 10)

(defn listings-page []
  (layout/render "listings/index.html" (conj (set-info) {:postages (postage/all (user-id)) :listings (listing/all (user-id))})))

(defn listing-remove [id]
  (let [record (listing/remove! id (user-id))]
  (if (nil? record)
    (resp/redirect "/market/")
  (do
    (session/flash-put! :success "listing removed")
    (resp/redirect "/market/listings")))))

(defn listing-edit [id]
  (let [listing (listing/get id)
        success (session/flash-get :success)]
    (layout/render "listings/create.html" (merge {:regions (region/all) :edit true :success success :id id :images (image/get (user-id)) :listing listing :categories (category/all) :currencies (currency/all)} (set-info) listing))))

(defn listing-save [{:keys [id image image_id] :as slug}]
  (let [listing (listing/update! (assoc slug :image_id (parse-image image_id image)) id (user-id))]
    (layout/render "listings/create.html" (merge {:regions (region/all) :id id :images (image/get (user-id)) :categories (category/all) :currencies (currency/all)} listing (set-info)))))

(defn listing-create
  "Listing creation page"
  ([]
   (layout/render "listings/create.html" (conj {:regions (region/all) :images (image/get (user-id)) :categories (category/all) :currencies (currency/all)} (set-info))))
  ([{:keys [image image_id] :as slug}]
   (let [listing (listing/add! (assoc slug :image_id (parse-image image_id image)) (user-id))]
     (if (empty? (:errors listing))
      (do
        (session/flash-put! :success "listing created")
        (resp/redirect (str "/market/listing/" (:id listing) "/edit")))
      (layout/render "listings/create.html" (merge {:regions (region/all) :images (image/get (user-id)) :categories (category/all) :currencies (currency/all)} (set-info) listing))))))

(defn listing-view [id page]
  (let [listing (listing/view id)
        page (or (util/parse-int page) 1)
        reviews (review/all id page per-page)
        revs (:reviews listing)
        pagemax (util/page-max revs per-page)]
    (layout/render "listings/view.html" (merge {:review reviews :page {:page page :max pagemax :url (str "/market/listing/" id)} :reported (report/reported? id (user-id) "listing") :bookmarked (bookmark/bookmarked? id (user-id))} (set-info) listing))))

(defn listing-bookmark [id]
  (if-let [bookmark (:errors (bookmark/add! id (user-id)))]
    (session/flash-put! :bookmark bookmark))
  (resp/redirect (str "/market/listing/" id)))

(defn listing-unbookmark [id referer]
  (bookmark/remove! id (user-id))
  (resp/redirect referer))

(def-restricted-routes listing-routes
    (GET "/market/listings" [] (listings-page))
    (GET "/market/listings/create" [] (listing-create))
    (GET "/market/listing/:id/bookmark" [id] (listing-bookmark id))
    (GET "/market/listing/:id/unbookmark" {{id :id} :params {referer "referer"} :headers} (listing-unbookmark id referer))
    (GET "/market/listing/:id/edit" [id] (listing-edit id))
    (GET "/market/listing/:id/report" {{id :id} :params {referer "referer"} :headers} (report-add id (user-id) "listing" referer))
    (GET "/market/listing/:id/unreport" {{id :id} :params {referer "referer"} :headers} (report-remove id (user-id) "listing" referer))
    (GET "/market/listing/:id" {{id :id page :page} :params} (listing-view id page))
    (GET "/market/listing/:id/remove" [id] (listing-remove id))
    (POST "/market/listing/:id/edit" {params :params} (listing-save params))
    (POST "/market/listings/create" {params :params} (listing-create params)))
