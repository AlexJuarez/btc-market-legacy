(ns whitecity.routes.account
  (:use compojure.core
        noir.util.route
        whitecity.helpers.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [whitecity.models.message :as message]
            [whitecity.models.listing :as listing]
            [whitecity.models.fan :as follower]
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

(defn account-page []
  (layout/render "account/index.html" (set-info)))

(defn account-update [{:keys [alias auth pub_key description] :as slug}]
  (let [user (user/update! (user-id) {:alias alias :auth auth :pub_key pub_key :description description})]
    (layout/render "account/index.html" (conj (set-info) user))))

(defn wallet-page [])

(defn images-page []
  (let [images (image/get (user-id))]
    (layout/render "images/index.html" (conj (set-info) {:images images}))))

(defn image-delete [id]
  (image/remove! id (user-id))
  (resp/redirect "/market/account/images"))

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
  (POST "/market/account" {params :params} (account-update params))
  (GET "/market/account/wallet" [] (wallet-page))
  (GET "/market/account/images" [] (images-page))
  (POST "/market/account/images/edit" {params :params} (images-edit params))
  (GET "/market/account/images/edit" [] (images-edit))
  (GET "/market/image/:id/delete" [id] (image-delete id))
  (GET "/market/user/:id/follow" [id] (user-follow id))
  (GET "/market/user/:id/unfollow" {{id :id} :params {referer "referer"} :headers} (user-unfollow id referer)))
