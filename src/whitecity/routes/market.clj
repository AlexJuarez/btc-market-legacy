(ns whitecity.routes.market
  (:use
    [compojure.core :only [GET POST]]
    [noir.util.route :only (def-restricted-routes)]
    [whitecity.helpers.route])
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

(def listings-per-page 20)

(def sort-options ["lowest" "highest" "title" "newest"])

(defn market-page [url {:keys [cid page sort_by ships_to ships_from] :as params}]
  (let [cid (or cid 1)
        categories (category/public cid)
        page (or (util/parse-int page) 1)
        pagemax (util/page-max (:count categories) listings-per-page)
        sort_by (some #{sort_by} sort-options)
        ships_to (= "true" ships_to)
        ships_from (= "true" ships_from)
        params (into {} (filter (comp identity second) {:sort_by sort_by :ships_to ships_to :ships_from ships_from}))
        listings (listing/public cid page listings-per-page params)]
    (layout/render "market/index.html"
                   (conj {:page {:page page :max pagemax :url url :params params}
                          :listings listings
                          :categories {:tree categories :params params}}
                         params
                         (set-info)))))

(defn home-page [params]
  (market-page "/market/" params))

(defn category-page [params]
  (market-page (str "/market/category/" (:cid params)) params))

(defn error-page []
  (layout/render "error.html"))

(defn about-page []
  (layout/render "about.html" (set-info)))

(defn user-view [id page]
  (let [user (user/get id)
        page (or (util/parse-int page) 1)
        description (util/md->html (:description user))
        listings (:listings user)
        pagemax (util/page-max listings user-listings-per-page)]
    (layout/render "users/view.html" (merge user {:page {:page page :max pagemax :url (str "/market/user/" id)} :listings-all (listing/public-for-user id page user-listings-per-page) :description description :feedback-rating (int (* (/ (:rating user) 5) 100)) :review (review/for-seller id) :reported (report/reported? id (user-id) "user") :followed (follower/followed? id (user-id))} (set-info) ))))

;;todo filters and stuff
(defn search-page [query]
  (if (and (>= (count query) 3) (<= (count query) 100))
    (let [q (str "%" query "%")
          users (user/search q)
          listings (listing/search q)
          categories (category/search q)
          message (if (and (empty? users) (empty? listings) (empty? categories)) "Nothing was found for your query. Please try again.")]
      (layout/render "market/search.html" (conj {:users users :listings listings :categories categories :query query :message message} (set-info))))
    (layout/render "market/search.html" (conj {:message "Your query is too short it needs to be longers than three characters and less than 100."} (set-info)))
    ))


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
    (layout/render "postage/create.html" (merge {:currencies (currency/all)} (set-info) postage))))

(defn postage-save [{:keys [id] :as slug}]
  (let [post (postage/update! slug id (user-id))]
    (layout/render "postage/create.html" (merge {:currencies (currency/all) :id id} post (set-info)))))

(defn postage-remove [id]
  (let [record (postage/remove! id (user-id))]
  (if (nil? record)
    (resp/redirect "/market/")
  (do (session/flash-put! :success {:success "postage removed"})
    (resp/redirect "/market/listings")))))

(defn feedback-page
  ([]
   (layout/render "feedback.html" (set-info))))

(defn resolution-accept [id referer]
  (resolution/accept id (user-id))
  (resp/redirect referer))

(def-restricted-routes market-routes
    (GET "/market/" {params :params} (home-page params))
    (GET "/market/search" {{q :q} :params} (search-page q))
    (GET "/market/resolution/:id/accept" {{id :id} :params {referer "referer"} :headers} (resolution-accept id referer))
    (GET "/market/category/:cid" {params :params} (category-page params))
    (GET "/market/postage/create" [] (postage-create))
    (GET "/market/postage/:id/edit" [id] (postage-edit id))
    (POST "/market/postage/:id/edit" {params :params} (postage-save params))
    (POST "/market/postage/create" {params :params} (postage-create params))
    (GET "/market/user/:id/report" {{id :id} :params {referer "referer"} :headers} (report-add id (user-id) "user" referer))
    (GET "/market/user/:id/unreport" {{id :id} :params {referer "referer"} :headers} (report-remove id (user-id) "user" referer))
    (GET "/market/user/:id" {{id :id page :page} :params} (user-view id page))
    (GET "/market/postage/:id/remove" [id] (postage-remove id))
    (GET "/market/about" [] (about-page))
    (GET "/market/feedback" [] (feedback-page))
  )
