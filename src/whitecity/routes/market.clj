(ns whitecity.routes.market
  (:use compojure.core
        noir.util.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [whitecity.models.message :as message]
            [whitecity.models.listing :as listing]
            [whitecity.models.image :as image]
            [noir.response :as resp]
            [clj-time.core :as cljtime]
            [clj-time.coerce :as tc]
            [clojure.string :as string]
            [noir.session :as session]
            [noir.io :as io]
            [whitecity.util :as util]))

(defn user-id []
  (:id (session/get :user)))

(defn set-info []
  {:user (conj (session/get :user) {:listing_count (listing/count (user-id)) :errors {} :orders 0 :messages (message/count (user-id)) :cart 0})})

(defn home-page []
  (layout/render "market.html" (set-info)))

(defn about-page []
  (layout/render "about.html"))

(defn messages-page []
  (layout/render "market/messages.html" (conj (set-info) {:messages (message/all (user-id))})))

(defn listings-page []
  (layout/render "market/listings.html" (conj (set-info) {:listings (listing/all (user-id))})))

(defn parse-image [image_id image]
  (if (and (not (nil? image)) (= 0 (:size image)))
    image_id
    (if (and (< (:size image) 800000) (not (empty? (re-find #"jpg|jpeg" (string/lower-case (:filename image))))))
      (let [image_id (:id (image/add! (user-id))) 
            image_result (io/upload-file "/uploads" (assoc image :filename (str image_id ".jpg")))]
          image_id))))

(defn listing-remove [id]
  (let [record (listing/remove! id (user-id))]
  (if (nil? record)
    (resp/redirect "/market/")
  (do (session/flash-put! :success {:success "listing removed"})
    (resp/redirect "/market/listings")))))

(defn listing-edit [id]
  (let [listing (listing/get id)]
    (layout/render "listings/edit.html" (conj {:images (image/get (user-id))} (set-info) listing))))

(defn listing-save [{:keys [id image image_id] :as slug}]
  (let [listing (listing/update! (assoc slug :image_id (parse-image image_id image)) id (user-id))]
    (layout/render "listings/edit.html" (conj {:id id :images (image/get (user-id))} listing (set-info)))))

(defn listing-create
  "Listing creation page" 
  ([]
   (layout/render "listings/create.html" (conj {:errors {} :images (image/get (user-id))} (set-info))))
  ([{:keys [image image_id] :as slug}]
   (let [listing (listing/add! (assoc slug :image_id (parse-image image_id image)) (user-id))]
     (if (nil? (:errors listing))
      (resp/redirect (str "/market/listing/" (:id listing) "/edit"))
      (layout/render "listings/create.html" (conj {:images (image/get (user-id))} (set-info) listing))))))

(def-restricted-routes market-routes
    (GET "/market/" [] (home-page))
    (GET "/market/messages" [] (messages-page))
    (GET "/market/listings" [] (listings-page))
    (GET "/market/listings/create" [] (listing-create))
    (GET "/market/listing/:id/edit" [id] (listing-edit id))
    (GET "/market/listing/:id/remove" [id] (listing-remove id))
    (POST "/market/listing/:id/edit" {params :params} (listing-save params))
    (POST "/market/listings/create" {params :params} (listing-create params))
    (GET "/market/about" [] (about-page)))
