(ns whitecity.routes.account
  (:use
    [compojure.core :only [GET POST context defroutes]]
    [noir.util.route :only (wrap-restricted restricted)]
    [whitecity.helpers.route])
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [whitecity.models.fan :as follower]
            [whitecity.models.audit :as audit]
            [whitecity.models.currency :as currency]
            [whitecity.models.review :as review]
            [whitecity.models.bookmark :as bookmark]
            [whitecity.models.post :as post]
            [whitecity.models.image :as image]
            [whitecity.models.region :as region]
            [noir.response :as resp]
            [noir.session :as session]
            [whitecity.util :as util]
            [whitecity.util.pgp :as pgp]))

(defonce reviews-per-page 25)

(defn account-page []
  (layout/render "account/index.html" (conj {:regions (region/all) :currencies (currency/all)} (set-info))))

(defn account-update [slug]
  (let [user (user/update! (user-id) slug)]
    (layout/render "account/index.html" (merge {:regions (region/all) :currencies (currency/all)} user (set-info)))))

(defn withdrawal [{:keys [amount address pin] :as slug}]
  (let [errors (:errors (user/withdraw-btc! slug (user-id)))
        user (util/current-user)
        transactions (audit/all (user-id))]
    (layout/render "account/wallet.html" (merge (set-info) {:amount amount :address address
                                                            :errors errors :transactions transactions
                                                            :balance (not (= (:currency_id user) 1))}))))
(defn change-pin [slug]
  (let [errors (:errors (user/update-pin! (user-id) slug))
        user (util/current-user)
        transactions (audit/all (user-id))]
    (layout/render "account/wallet.html" (merge (set-info)
                                                (if (empty? errors) {:pin-success "Your pin has been changed"})
                                                {:pinerrors errors :transactions transactions
                                                 :balance (not (= (:currency_id user) 1))}))))

(defn wallet-page
  ([]
   (let [user (util/current-user)
         transactions (audit/all (user-id))]
     (layout/render "account/wallet.html" (merge (set-info) {:transactions transactions :balance (not (= (:currency_id user) 1))})))
   )
  ([slug]
   (if (not (nil? (:confirmpin slug)))
     (change-pin slug)
     (withdrawal slug))))

(defn wallet-new []
  (user/update-btc-address! (user-id))
  (resp/redirect "/account/wallet"))

(defn favorites-page []
  (let [bookmarks (map #(assoc % :price (util/convert-currency %)) (bookmark/all (user-id)))
        favs (follower/all (user-id))]
    (layout/render "account/favorites.html" (merge (set-info) {:bookmarks bookmarks :favorites favs}))))

(defn reviews-page
  ([page]
   (let [page (or (util/parse-int page) 1)
         reviews (review/for-user (user-id) page reviews-per-page)
         success (session/flash-get :success)
         pagemax (util/page-max (:reviewed (util/current-user)) reviews-per-page)]
     (layout/render "account/reviews.html" (conj (set-info) {:success success :reviews reviews :page {:page page :max pagemax :url "/account/reviews"}})))))

(defn review-edit
  ([id]
   (let [review (review/get id (user-id))]
     (layout/render "review/edit.html" (conj (set-info) review))))
  ([id slug]
   (review/update! id slug (user-id))
   (session/flash-put! :success "review updated")
   (resp/redirect "/account/reviews")))

(defn images-page []
  (let [images (image/get (user-id)) success (session/flash-get :success)]
    (layout/render "images/index.html" (conj (set-info) {:images images :success success}))))

(defn images-upload
  ([]
    (layout/render "images/upload.html" (set-info)))
  ([{image :image}]
   (parse-image nil image)
   (session/flash-put! :success "image uploaded")
   (resp/redirect "/account/images")))

(defn image-delete [id]
  (image/remove! id (user-id))
  (session/flash-put! :success "image deleted")
  (resp/redirect "/account/images"))

(defn password-page
  ([]
    (layout/render "account/password.html" (set-info)))
  ([slug]
   (let [errors (user/update-password! (user-id) slug)
         message (if (empty? errors) "You have successfully changed your password")]
    (layout/render "account/password.html" (merge (set-info) {:message message :errors errors})))))

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
  (resp/redirect (str "/user/" id)))

(defn user-unfollow [id referer]
  (follower/remove! id (user-id))
  (resp/redirect referer))

(defn news-page []
  (let [posts (post/all (user-id))]
      (layout/render "news/index.html" (merge {:posts posts} (set-info)))))

(defn news-create
  ([]
   (layout/render "news/create.html" (merge (set-info))))
  ([slug]
   (let [post (post/add! slug (user-id))]
     (if (empty? (:errors post))
       (resp/redirect (str "/vendor/news/" (:id post) "/edit"))
       (layout/render "news/create.html" (merge (set-info) post))))))

(defn news-publish [id]
  (post/publish! id (user-id))
  (resp/redirect "/vendor/news"))

(defn news-delete [id]
  (post/remove! id (user-id))
  (resp/redirect "/vendor/news"))

(defn news-view [id]
  (let [article (post/get id)
        content (util/md->html (:content article))]
    (layout/render "news/view.html" (merge (set-info) article {:content content}))
  ))

(defn news-edit
  ([id]
   (let [id (util/parse-int id)
         article (post/get id)
         content (util/md->html (:content article))]
     (layout/render "news/create.html" (merge (set-info) article {:preview content}))))
  ([id slug]
   (let [id (util/parse-int id)
         article (post/update! slug (user-id))
         content (util/md->html (:content article))]
     (layout/render "news/create.html" (merge (set-info) article {:preview content})))))

(defn pgp-page
  ([]
   (layout/render "account/pgp.html" (assoc (set-info) :message (session/flash-get :message))))
  ([{:keys [pub_key verification] :as slug}]
   (let [errors (user/valid-pgp? slug)
         message (session/flash-get :pgp-message)]
     (if (empty? errors)
       (do
         (session/put! :pub_key pub_key)
         (resp/redirect "/account/pgp/verify"))
       (layout/render "account/pgp.html" (merge (set-info) {:errors errors :pub_key pub_key}))))))

(defn pgp-verify
  ([]
   (let [pub_key (session/get :pub_key)
         secret (util/generate-salt)
         decode (pgp/encode pub_key secret)
         message (session/flash-get :message)]
     (session/flash-put! :pgp-verify secret)
     (layout/render "account/pgp-verification.html" (merge (set-info) {:message message :decode decode :pub_key pub_key})))
   )
  ([{response :response}]
   (let [secret (session/flash-get :pgp-verify)]
     (if (= secret response)
       (do
         (session/flash-put! :message "success")
         (user/update-pgp! (session/get :pub_key))
         (pgp-page))
       (do
         (session/flash-put! :message "please try again")
         (pgp-verify))))))

(defroutes account-routes
  (wrap-restricted
   (context
    "/account" []
    (GET "/" [] (account-page))
    (POST "/" {params :params} (account-update params))
    (GET "/pgp" [] (pgp-page))
    (POST "/pgp" {params :params} (pgp-page params))
    (GET "/pgp/verify" [] (pgp-verify))
    (POST "/pgp/verify" {params :params} (pgp-verify params))
    (GET "/password" [] (password-page))
    (POST "/password" {params :params} (password-page params))
    (GET "/wallet" [] (wallet-page))
    (POST "/wallet" {params :params} (wallet-page params))
    (GET "/wallet/new" [] (wallet-new))
    (GET "/favorites" [] (favorites-page))
    (GET "/reviews" [page] (reviews-page page))))

  (GET "/news/:id" [id] (restricted (news-view id)))

  (wrap-restricted
   (context
    "/vendor" []
    (GET "/news" [] (news-page))
    (GET "/news/create" [] (news-create))
    (POST "/news/create" {params :params} (news-create params))
    (GET "/news/:id/edit" [id] (news-edit id))
    (POST "/news/:id/edit" {params :params} (news-edit (:id params) params))
    (GET "/news/:id/publish" [id] (news-publish id))
    (GET "/news/:id/delete" [id] (news-delete id))
    (GET "/images" [] (images-page))
    (POST "/images/edit" {params :params} (images-edit params))
    (GET "/images/edit" [] (images-edit))
    (GET "/image/:id/delete" [id] (image-delete id))
    (POST "/images/upload" {params :params} (images-upload params))
    (GET "/images/upload" [] (images-upload))))

  (wrap-restricted
   (context
    "/review/:id" [id]
    (GET "/edit" [] (review-edit id))
    (POST "/edit" {params :params} (review-edit (:id params) params))))

  (wrap-restricted
   (context
    "/user/:id" [id]
    (GET "/follow" [] (user-follow id))
    (GET "/unfollow" {{referer "referer"} :headers} (user-unfollow id referer)))))
