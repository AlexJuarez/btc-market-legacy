(ns whitecity.routes.market
  (:use compojure.core
        noir.util.route
        whitecity.helpers.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [whitecity.models.message :as message]
            [whitecity.models.listing :as listing]
            [whitecity.models.image :as image]
            [whitecity.models.bookmark :as bookmark]
            [whitecity.models.postage :as postage]
            [whitecity.models.currency :as currency]
            [whitecity.models.order :as order]
            [noir.response :as resp]
            [clojure.string :as string]
            [noir.session :as session]
            [noir.io :as io]
            [whitecity.util :as util]))

(defn home-page []
  (layout/render "market/index.html" (conj {:listings (listing/public)} (set-info))))

(defn about-page []
  (layout/render "about.html"))

(defn messages-page []
  (layout/render "messages/index.html" (conj (set-info) {:messages (message/all (user-id))})))

(defn messages-sent []
  (layout/render "messages/sent.html" (conj (set-info) {:messages (message/sent (user-id))})))

(defn messages-thread
  ([receiver-id]
   (layout/render "messages/thread.html" (merge (set-info) {:user_id receiver-id :messages (message/all (user-id) receiver-id)})))
  ([slug &options]
   (let [message (message/add! slug (user-id) (:id slug))]
     (layout/render "messages/thread.html" (merge (set-info) {:messages (message/all (user-id) (:id slug))} message)))))

(defn listings-page []
  (layout/render "listings/index.html" (merge (set-info) {:postages (postage/all (user-id)) :listings (listing/all (user-id))})))

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
    (layout/render "listings/edit.html" (merge {:images (image/get (user-id)) :listing listing :currencies (currency/all)} (set-info) listing))))

(defn listing-save [{:keys [id image image_id] :as slug}]
  (let [listing (listing/update! (assoc slug :image_id (parse-image image_id image)) id (user-id))]
    (layout/render "listings/edit.html" (merge {:id id :images (image/get (user-id)) :currencies (currency/all)} listing (set-info)))))

(defn listing-create
  "Listing creation page" 
  ([]
   (layout/render "listings/create.html" (conj {:images (image/get (user-id)) :currencies (currency/all)} (set-info))))
  ([{:keys [image image_id] :as slug}]
   (let [listing (listing/add! (assoc slug :image_id (parse-image image_id image)) (user-id))]
     (if (empty? (:errors listing))
      (resp/redirect (str "/market/listing/" (:id listing) "/edit"))
      (layout/render "listings/create.html" (merge {:images (image/get (user-id)) :currencies (currency/all)} (set-info) listing))))))

(defn profile-view [id]
  (let [user (user/get id)]
    (layout/render "users/view.html" (merge {:listings (listing/public id)} (set-info) user))))

(defn listing-view [id]
  (let [listing (listing/view id)]
    (layout/render "listings/view.html" (merge (set-info) listing))))

(defn listing-bookmark [id]
  (if-let [bookmark (:errors (bookmark/add! id (user-id)))]
    (session/flash-put! :bookmark bookmark))
  (resp/redirect "/market/account/bookmarks"))

(defn postage-create
  ([]
   (layout/render "postage/create.html" (merge {:currencies (currency/all)} (set-info))))
  ([slug]
   (let [post (postage/add! slug (user-id))]
     (if (empty? (:errors post))
       (resp/redirect "/market/listings")
       (layout/render "postage/create.html" (merge {:currencies (currency/all)} (set-info) post))))))

(defn postage-edit [id]
  (let [postage (postage/get id (user-id))]
    (layout/render "postage/edit.html" (merge {:currencies (currency/all)} (set-info) postage))))

(defn postage-save [{:keys [id] :as slug}]
  (let [post (postage/update! slug id (user-id))]
    (layout/render "postage/edit.html" (merge {:currencies (currency/all) :id id} post (set-info)))))

(defn postage-remove [id]
  (let [record (postage/remove! id (user-id))]
  (if (nil? record)
    (resp/redirect "/market/")
  (do (session/flash-put! :success {:success "postage removed"})
    (resp/redirect "/market/listings")))))

(defn orders-page []
  (let [orders (map #(let [autofinalize (java.sql.Timestamp. (+ 1468800000 (.getTime (:created_on %))))] 
                         (conj % {:auto_finalize autofinalize})) (order/all (user-id)))]
     (layout/render "orders/index.html" (merge {:errors {} :orders orders :user-id (user-id)} (set-info)))))

(def-restricted-routes market-routes
    (GET "/market/" [] (home-page))
    (GET "/market/messages" [] (messages-page))
    (GET "/market/messages/sent" [] (messages-sent))
    (GET "/market/messages/:id" [id] (messages-thread id))
    (GET "/market/orders" [] (orders-page))
    (POST "/market/messages/:id" {params :params} (messages-thread params true))
    (GET "/market/postage/create" [] (postage-create))
    (GET "/market/postage/:id/edit" [id] (postage-edit id))
    (POST "/market/postage/:id/edit" {params :params} (postage-save params))
    (POST "/market/postage/create" {params :params} (postage-create params))
    (GET "/market/listings" [] (listings-page))
    (GET "/market/listings/create" [] (listing-create))
    (GET "/market/listing/:id/bookmark" [id] (listing-bookmark id))
    (GET "/market/listing/:id/edit" [id] (listing-edit id))
    (GET "/market/user/:id" [id] (profile-view id))
    (GET "/market/listing/:id" [id] (listing-view id))
    (GET "/market/listing/:id/remove" [id] (listing-remove id))
    (GET "/market/postage/:id/remove" [id] (postage-remove id))
    (POST "/market/listing/:id/edit" {params :params} (listing-save params))
    (POST "/market/listings/create" {params :params} (listing-create params))
    (GET "/market/about" [] (about-page)))
