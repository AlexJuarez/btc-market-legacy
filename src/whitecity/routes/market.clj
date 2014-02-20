(ns whitecity.routes.market
  (:use compojure.core
        noir.util.route
        whitecity.helpers.route)
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [whitecity.models.message :as message]
            [whitecity.models.listing :as listing]
            [whitecity.models.category :as category]
            [whitecity.models.report :as report]
            [whitecity.models.review :as review]
            [whitecity.models.fan :as follower]
            [whitecity.models.postage :as postage]
            [whitecity.models.currency :as currency]
            [noir.response :as resp]
            [noir.session :as session]
            [whitecity.util :as util]))


(defn home-page []
  (layout/render "market/index.html" (conj {:listings (listing/public) :categories (category/public 1)} (set-info))))

(defn category-page [cid]
  (layout/render "market/index.html" (conj {:listings (listing/public cid) :categories (category/public cid)} (set-info))))

(defn about-page []
  (layout/render "about.html"))

(defn messages-page []
  (layout/render "messages/index.html" (conj (set-info) {:messages (message/all (user-id))})))

(defn messages-sent []
  (layout/render "messages/sent.html" (conj (set-info) {:messages (message/sent (user-id))})))

(defn message-delete [id]
  (message/remove! id (user-id))
  (resp/redirect "/market/messages"))

(defn messages-thread
  ([receiver-id]
   (layout/render "messages/thread.html" (merge (set-info) {:user_id receiver-id :messages (message/all (user-id) receiver-id)})))
  ([slug & options]
   (let [message (message/add! slug (user-id) (:id slug))]
     (layout/render "messages/thread.html" (merge (set-info) {:messages (message/all (user-id) (:id slug))} message)))))

(defn user-view [id]
  (let [user (user/get id) description (util/md->html (:description user))]
    (layout/render "users/view.html" (merge user {:listings-all (listing/public-for-user id) :description description :feedback-rating (int (* (/ (:rating user) 5) 100)) :review (review/for-user id) :reported (report/reported? id (user-id) "user") :followed (follower/followed? id (user-id))} (set-info) ))))

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

(def-restricted-routes market-routes
    (GET "/market/" [] (home-page))
    (GET "/market/category/:cid" [cid] (category-page cid))
    (GET "/market/message/:id/delete" [id] (message-delete id))
    (GET "/market/messages" [] (messages-page))
    (GET "/market/messages/sent" [] (messages-sent))
    (GET "/market/messages/:id" [id] (messages-thread id))
    (POST "/market/messages/:id" {params :params} (messages-thread params true))
    (GET "/market/postage/create" [] (postage-create))
    (GET "/market/postage/:id/edit" [id] (postage-edit id))
    (POST "/market/postage/:id/edit" {params :params} (postage-save params))
    (POST "/market/postage/create" {params :params} (postage-create params))
    (GET "/market/user/:id/report" {{id :id} :params {referer "referer"} :headers} (report-add id (user-id) "user" referer))
    (GET "/market/user/:id/unreport" {{id :id} :params {referer "referer"} :headers} (report-remove id (user-id) "user" referer))
    (GET "/market/user/:id" [id] (user-view id))
    (GET "/market/postage/:id/remove" [id] (postage-remove id))
    (GET "/market/about" [] (about-page)))
