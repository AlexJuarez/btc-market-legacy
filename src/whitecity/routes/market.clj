(ns whitecity.routes.market
  (:use compojure.core
        noir.util.route
        whitecity.helpers.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [whitecity.models.message :as message]
            [whitecity.models.listing :as listing]
            [whitecity.models.category :as category]
            [whitecity.models.resolution :as resolution]
            [whitecity.models.report :as report]
            [whitecity.models.review :as review]
            [whitecity.models.fan :as follower]
            [whitecity.models.postage :as postage]
            [whitecity.models.currency :as currency]
            [noir.response :as resp]
            [noir.session :as session]
            [whitecity.util :as util]))

(def user-per-page 10)

(def user-listings-per-page 10)

(defn home-page [page]
  (let [page (or (util/parse-int page) 1)]
  (layout/render "market/index.html" (conj {:listings (listing/public) :categories (category/public 1)} (set-info)))))

(defn category-page [cid page]
  (layout/render "market/index.html" (conj {:listings (listing/public cid) :categories (category/public cid)} (set-info))))

(defn about-page []
  (layout/render "about.html"))

(defn user-view [id page]
  (let [user (user/get id) 
        page (or (util/parse-int page) 1)
        description (util/md->html (:description user))
        listings (:listings user)
        pagemax (util/page-max listings user-listings-per-page)]
    (layout/render "users/view.html" (merge user {:page {:page page :max pagemax :url (str "/market/user/" id)} :listings-all (listing/public-for-user id page user-listings-per-page) :description description :feedback-rating (int (* (/ (:rating user) 5) 100)) :review (review/for-user id) :reported (report/reported? id (user-id) "user") :followed (follower/followed? id (user-id))} (set-info) ))))

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

(defn resolution-accept [id referer]
  (resolution/accept id (user-id))
  (resp/redirect referer))

(def-restricted-routes market-routes
    (GET "/market/" {{page :page} :params} (home-page page))
    (GET "/market/resolution/:id/accept" {{id :id} :params {referer "referer"} :headers} (resolution-accept id referer))
    (GET "/market/category/:cid" {{page :page cid :cid} :params} (category-page cid page))
    (GET "/market/postage/create" [] (postage-create))
    (GET "/market/postage/:id/edit" [id] (postage-edit id))
    (POST "/market/postage/:id/edit" {params :params} (postage-save params))
    (POST "/market/postage/create" {params :params} (postage-create params))
    (GET "/market/user/:id/report" {{id :id} :params {referer "referer"} :headers} (report-add id (user-id) "user" referer))
    (GET "/market/user/:id/unreport" {{id :id} :params {referer "referer"} :headers} (report-remove id (user-id) "user" referer))
    (GET "/market/user/:id" {{id :id page :page} :params} (user-view id page))
    (GET "/market/postage/:id/remove" [id] (postage-remove id))
    (GET "/market/about" [] (about-page)))
