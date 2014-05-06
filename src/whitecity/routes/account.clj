(ns whitecity.routes.account
  (:use 
    [compojure.core :only [GET POST]]
    [noir.util.route :only (def-restricted-routes)]
    [whitecity.helpers.route])
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [whitecity.models.fan :as follower]
            [whitecity.models.currency :as currency]
            [whitecity.models.review :as review]
            [whitecity.models.bookmark :as bookmark]
            [whitecity.models.image :as image]
            [whitecity.models.region :as region]
            [noir.response :as resp]
            [noir.session :as session]
            [whitecity.util :as util]))

(defonce reviews-per-page 25)

(defn account-page []
  (layout/render "account/index.html" (conj {:regions (region/all) :currencies (currency/all)} (set-info))))

(defn account-update [slug]
  (let [user (user/update! (user-id) slug)]
    (layout/render "account/index.html" (merge {:regions (region/all) :currencies (currency/all)} (set-info) user))))

(defn wallet-page []
  (let []
  (layout/render "account/wallet.html" (merge (set-info) {}))))

(defn favorites-page []
  (let [bookmarks (bookmark/all (user-id))
        favs (follower/all (user-id))]
    (layout/render "account/favorites.html" (merge (set-info) {:bookmarks bookmarks :favorites favs}))))

(defn reviews-page
  ([]
   (let [page (or (util/parse-int page) 1)
         reviews (review/for-user (user-id) page reviews-per-page)
         pagemax (util/page-max (:reviewed (current-user)) reviews-per-page)])))


(defn images-page []
  (let [images (image/get (user-id))]
    (layout/render "images/index.html" (conj (set-info) {:images images}))))

(defn image-delete [id]
  (image/remove! id (user-id))
  (resp/redirect "/market/account/images"))

(defn password-page 
  ([]
    (layout/render "account/password.html" (set-info)))
  ([slug]
   (let [errors (user/update-password! (user-id) slug)]
    (layout/render "account/password.html" (merge (set-info) (if (nil? errors) {:message "success"})  errors)))))
 
(defn images-edit 
  ([]
    (let [images (image/get (user-id))]
      (layout/render "images/edit.html" (conj (set-info) {:images images}))))
  ([{:keys [name] :as slug}]
    (dorun (map #(image/update! (key %) {:name (val %)}) name))
    (images-edit)))

(defn user-follow [id]
  (if-let [follower (:errors (follower/add! id (user-id)))]
    (session/flash-put! :follower follower))
  (resp/redirect (str "/market/user/" id)))

(defn user-unfollow [id referer]
  (follower/remove! id (user-id))
  (resp/redirect referer))

(def-restricted-routes account-routes
  (GET "/market/account" [] (account-page))
  (GET "/market/account/password" [] (password-page))
  (POST "/market/account/password" {params :params} (password-page params))
  (POST "/market/account" {params :params} (account-update params))
  (GET "/market/account/wallet" [] (wallet-page))
  (GET "/market/account/images" [] (images-page))
  (GET "/market/account/favorites" [] (favorites-page))
  (GET "/market/account/reviews" [] (reviews-page))
  (POST "/market/account/images/edit" {params :params} (images-edit params))
  (GET "/market/account/images/edit" [] (images-edit))
  (GET "/market/image/:id/delete" [id] (image-delete id))
  (GET "/market/user/:id/follow" [id] (user-follow id))
  (GET "/market/user/:id/unfollow" {{id :id} :params {referer "referer"} :headers} (user-unfollow id referer)))
