(ns whitecity.routes.market
  (:use
   [compojure.core :only [GET POST context defroutes]]
   [noir.util.route :only (wrap-restricted restricted)]
   [whitecity.helpers.route])
  (:require
   [whitecity.views.layout :as layout]
   [ring.util.response :as r :refer [content-type response]]
   [whitecity.models.user :as user]
   [whitecity.models.feedback :as feedback]
   [whitecity.models.listing :as listing]
   [whitecity.models.bookmark :as bookmark]
   [whitecity.models.category :as category]
   [whitecity.models.resolution :as resolution]
   [whitecity.models.report :as report]
   [whitecity.models.review :as review]
   [whitecity.models.fan :as follower]
   [whitecity.models.post :as post]
   [noir.response :as resp]
   [noir.session :as session]
   [whitecity.util :as util]))

(def per-page 10)

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
  (market-page "/" params))

(defn category-page [params]
  (market-page (str "/category/" (:cid params)) params))

(defn error-page []
  (layout/render "error.html"))

(defn about-page []
  (layout/render "about.html" (set-info)))

(defn user-key [id]
  (let [user (user/get id)]
    (-> (response (:pub_key user))
        (content-type "text/plain")
        (r/header "Content-Disposition" (str "attachment;filename=" (:pub_key_id user) ".asc")))))

(defn user-view [id page]
  (let [user (user/get id)
        id (:id user)
        page (or (util/parse-int page) 1)
        description (util/md->html (:description user))
        listings (:listings user)
        pagemax (util/page-max listings user-listings-per-page)]
    (layout/render "users/view.html" (merge user {:page {:page page :max pagemax :url (str "/user/" id)}
                                                  :listings-all (listing/public-for-user id page user-listings-per-page)
                                                  :description description
                                                  :posts (post/get-updates id)
                                                  :feedback-rating (int (* (/ (:rating user) 5) 100))
                                                  :review (review/for-seller id)
                                                  :reported (report/reported? id (user-id) "user")
                                                  :followed (follower/followed? id (user-id))} (set-info) ))))

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

(defn support-page
  ([]
   (layout/render "support.html" (set-info)))
  ([slug]
   (let [post (feedback/add! slug (user-id))])
   (layout/render "support.html" (conj {:message "Your request for support has been recieved."} (set-info)) )))

(defn api-vendors [api_key]
  (resp/json (map #(assoc % :uri (str "/user/" (:alias %))) (user/vendor-list))))

(defn api-listings [params sign]
  )

(defn listing-view [id page]
  (let [listing (listing/view id)
        page (or (util/parse-int page) 1)
        reviews (review/all id page per-page)
        revs (:reviews listing)
        description (util/md->html (:description listing))
        pagemax (util/page-max revs per-page)]
    (layout/render "listings/view.html" (merge {:review reviews :page {:page page :max pagemax :url (str "/listing/" id)} :reported (report/reported? id (user-id) "listing") :bookmarked (bookmark/bookmarked? id (user-id))} (set-info) listing {:description description}))))

(defn resolution-accept [id referer]
  (resolution/accept id (user-id))
  (resp/redirect referer))

(defroutes market-routes
  (GET "/" {params :params} (home-page params))
  (GET "/search" {{q :q} :params} (search-page q))

  (GET "/category/:cid" {params :params} (category-page params))
  (GET "/about" [] (about-page))
  (GET "/support" [] (support-page))
  (POST "/support" {params :params} (support-page params))

  (GET "/api/vendors" [api_key] (api-vendors api_key))
  (GET "/api/listings" {params :params {sign "sign"} :headers} (api-listings params sign))

  ;;public routes
  (GET "/user/:id" {{id :id page :page} :params} (user-view id page))
  (GET "/user/:id/key" [id] (user-key id))
  (GET "/listing/:id" {{id :id page :page} :params} (listing-view id page))

  ;;restricted routes
  (GET "/resolution/:id/accept" {{id :id} :params {referer "referer"} :headers} (restricted (resolution-accept id referer)))

  (wrap-restricted
   (context
    "/user/:id" [id]
    (GET "/report" {{id :id} :params {referer "referer"} :headers} (report-add id (user-id) "user" referer))
    (GET "/unreport" {{id :id} :params {referer "referer"} :headers} (report-remove id (user-id) "user" referer)))))
