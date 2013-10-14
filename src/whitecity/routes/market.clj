(ns whitecity.routes.market
  (:use compojure.core
        noir.util.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [whitecity.models.message :as message]
            [whitecity.models.listing :as listing]
            [whitecity.models.image :as image]
            [whitecity.models.postage :as postage]
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
  {:user (merge 
           (session/get :user) 
           {:listing_count (listing/count (user-id)) 
            :errors {} 
            :orders 0 
            :messages (message/count (user-id)) :cart 0})})

(defn home-page []
  (layout/render "market/index.html" (conj {:listings (listing/public)} (set-info))))

(defn about-page []
  (layout/render "about.html"))

(defn messages-page []
  (layout/render "market/messages.html" (conj (set-info) {:messages (message/all (user-id))})))

(defn listings-page []
  (layout/render "market/listings.html" (conj (set-info) {:postages (postage/all (user-id)) :listings (listing/all (user-id))})))

;;TODO add thumbnail parsing with imagez
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
    (layout/render "listings/edit.html" (merge {:images (image/get (user-id))} (set-info) listing))))

(defn listing-save [{:keys [id image image_id] :as slug}]
  (let [listing (listing/update! (assoc slug :image_id (parse-image image_id image)) id (user-id))]
    (layout/render "listings/edit.html" (merge {:id id :images (image/get (user-id))} listing (set-info)))))

(defn listing-create
  "Listing creation page" 
  ([]
   (layout/render "listings/create.html" (conj {:images (image/get (user-id))} (set-info))))
  ([{:keys [image image_id] :as slug}]
   (let [listing (listing/add! (assoc slug :image_id (parse-image image_id image)) (user-id))]
     (if (empty? (:errors listing))
      (resp/redirect (str "/market/listing/" (:id listing) "/edit"))
      (layout/render "listings/create.html" (merge {:images (image/get (user-id))} (set-info) listing))))))

(defn profile-view [id]
  (let [user (user/get id)]
    (layout/render "users/view.html" (merge {:listings (listing/public id)} (set-info) user))))

(defn listing-view [id]
  (let [listing (listing/view id)]
    (layout/render "listings/view.html" (merge {:postages (postage/all (:user_id listing))} (set-info) listing))))

(defn postage-create
  ([]
   (layout/render "postage/create.html" (set-info)))
  ([slug]
   (let [post (postage/add! slug (user-id))]
     (if (empty? (:errors post))
       (resp/redirect (str "/market/postage/" (:id post) "/edit"))
       (layout/render "postage/create.html" (merge (set-info) post))))))

(defn postage-edit [id]
  (let [postage (postage/get id (user-id))]
    (layout/render "postage/edit.html" (merge (set-info) postage))))

(defn postage-save [{:keys [id] :as slug}]
  (let [post (postage/update! slug id (user-id))]
    (layout/render "postage/edit.html" (merge {:id id} post (set-info)))))

(defn postage-remove [id]
  (let [record (postage/remove! id (user-id))]
  (if (nil? record)
    (resp/redirect "/market/")
  (do (session/flash-put! :success {:success "postage removed"})
    (resp/redirect "/market/listings")))))

(defn add-to-cart
  "add a item to the cart"
  [id]
  (do (session/put! :cart (conj [] (session/get :cart) id))))


(def-restricted-routes market-routes
    (GET "/market/" [] (home-page))
    (GET "/market/messages" [] (messages-page))
    (GET "/market/postage/create" [] (postage-create))
    (GET "/market/postage/:id/edit" [id] (postage-edit id))
    (POST "/market/postage/:id/edit" {params :params} (postage-save params))
    (POST "/market/postage/create" {params :params} (postage-create params))
    (GET "/market/listings" [] (listings-page))
    (GET "/market/listings/create" [] (listing-create))
    (GET "/market/listing/:id/edit" [id] (listing-edit id))
    (GET "/market/user/:id" [id] (profile-view id))
    (GET "/market/listing/:id" [id] (listing-view id))
    (GET "/market/listing/:id/remove" [id] (listing-remove id))
    (GET "/market/postage/:id/remove" [id] (postage-remove id))
    (POST "/market/listing/:id/edit" {params :params} (listing-save params))
    (POST "/market/listings/create" {params :params} (listing-create params))
    (GET "/market/listing/:id/add" [id] (add-to-cart id))
    (GET "/market/about" [] (about-page)))
