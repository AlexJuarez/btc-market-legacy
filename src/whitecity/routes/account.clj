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

(defn account-page [])

(defn wallet-page [])

(defn user-follow [id]
  (if-let [follower (:errors (follower/add! id (user-id)))]
    (session/flash-put! :follower follower))
  (resp/redirect (str "/market/user/" id)))

(defn user-unfollow [id referer]
  (follower/remove! id (user-id))
  (resp/redirect referer))

(def-restricted-routes account-routes
  (GET "/market/account" [] (account-page))
  (GET "/market/acount/wallet" [] (wallet-page))
  (GET "/market/user/:id/follow" [id] (user-follow id))
  (GET "/market/user/:id/unfollow" {{id :id} :params {referer "referer"} :headers} (user-unfollow id referer)))
