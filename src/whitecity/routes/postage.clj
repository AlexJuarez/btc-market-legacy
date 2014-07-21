(ns whitecity.routes.postage
   (:use
    [whitecity.helpers.route]
    [compojure.core :only [GET POST context defroutes]]
    [noir.util.route :only (wrap-restricted)])
   (:require
    [whitecity.views.layout :as layout]
    [whitecity.models.currency :as currency]
    [whitecity.models.postage :as postage]
    [noir.response :as resp]
    [noir.session :as session]
))

(defn postage-create
  ([]
   (layout/render "postage/create.html" (merge {:currencies (currency/all)} (set-info))))
  ([slug]
   (let [post (postage/add! slug (user-id))]
     (if (empty? (:errors post))
       (resp/redirect "/vendor/listings")
       (layout/render "postage/create.html" (merge {:currencies (currency/all)} (set-info) post))))))

(defn postage-edit [id]
  (let [postage (postage/get id (user-id))]
    (layout/render "postage/create.html" (merge {:currencies (currency/all)} (set-info) postage))))

(defn postage-save [{:keys [id] :as slug}]
  (let [post (postage/update! slug id (user-id))]
    (layout/render "postage/create.html" (merge {:currencies (currency/all) :id id} post (set-info)))))

(defn postage-remove [id]
  (let [record (postage/remove! id (user-id))]
  (if (nil? record)
    (resp/redirect "/vendor/listings")
  (do (session/flash-put! :success {:success "postage removed"})
    (resp/redirect "/vendor/listings")))))

(defroutes postage-routes
  (wrap-restricted
   (context
    "/vendor/postage" []
    (GET "/create" [] (postage-create))
    (POST "/create" {params :params} (postage-create params))
    (GET "/:id/edit" [id] (postage-edit id))
    (POST "/:id/edit" {params :params} (postage-save params))
    (GET "/:id/remove" [id] (postage-remove id)))))
