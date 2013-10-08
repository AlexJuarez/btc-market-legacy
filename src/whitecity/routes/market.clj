(ns whitecity.routes.market
  (:use compojure.core)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [whitecity.models.message :as message]
            [noir.session :as session]
            [whitecity.util :as util]))

(defn set-info []
  {:user (session/get :user) :orders 0 :messages (message/get-messages-count (:id (session/get :user))) :cart 0})

(defn home-page []
  (layout/render "market.html" (set-info)))

(defn about-page []
  (layout/render "about.html"))

(defroutes market-routes
  (GET "/market/" [] (home-page))
  (GET "/market/about" [] (about-page)))
