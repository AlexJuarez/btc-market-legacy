(ns whitecity.routes.message
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
            [whitecity.models.post :as post]
            [whitecity.models.currency :as currency]
            [noir.response :as resp]
            [noir.session :as session]
            [whitecity.util :as util]))

(defonce per-page 25)

(defn messages-page [page]
  (let [page (or (util/parse-int page) 1)
        pagemax (util/page-max (:total (util/session! :messages (message/count (user-id)))) per-page)
        news (post/get-news (user-id)) ;;TODO: add pagination
        messages (message/all (user-id) page per-page)]
    (layout/render "messages/index.html" (conj (set-info) {:page {:page page :max pagemax}
                                                           :news news
                                                           :messages messages}))))

(defn messages-sent []
  (layout/render "messages/sent.html" (conj (set-info) {:messages (message/sent (user-id))})))

(defn message-delete [id]
  (message/remove! id (user-id))
  (resp/redirect "/messages"))

(defn messages-thread
  ([receiver-id]
   (layout/render "messages/thread.html" (merge (set-info)
                                                {:user_id receiver-id :alias (:alias (user/get receiver-id)) :messages (message/all (user-id) receiver-id)})))
  ([slug & options]
   (let [message (message/add! slug (user-id) (:id slug))]
     (layout/render "messages/thread.html" (merge (set-info)
                                                  {:user_id (:id slug) :messages (message/all (user-id) (:id slug))} message)))))

(def-restricted-routes message-routes
    (GET "/message/:id/delete" [id] (message-delete id))
    (GET "/messages" [page] (messages-page page))
    (GET "/messages/sent" [] (messages-sent))
    (GET "/messages/:id" [id] (messages-thread id))
    (POST "/messages/:id" {params :params} (messages-thread params true)))
