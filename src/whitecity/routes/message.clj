(ns whitecity.routes.message
  (:use
    [compojure.core :only [GET POST]]
    [noir.util.route :only (def-restricted-routes)]
    [whitecity.helpers.route])
  (:require [whitecity.views.layout :as layout]
            [whitecity.models.user :as user]
            [ring.util.response :as r :refer [content-type response]]
            [whitecity.models.message :as message]
            [whitecity.models.post :as post]
            [clojure.string :as string]
            [whitecity.models.currency :as currency]
            [noir.response :as resp]
            [noir.session :as session]
            [whitecity.util :as util]
            [whitecity.util.hashids :as hashids]))

(defonce per-page 25)

(defn encrypt-feedback-ids [messages]
  (map #(assoc % :feedback_id (hashids/encrypt-ticket-id (:feedback_id %))) messages))

(defn messages-page [page]
  (let [page (or (util/parse-int page) 1)
        pagemax (util/page-max (:total (util/session! :messages (message/count (user-id)))) per-page)
        news (post/get-news (user-id)) ;;TODO: add pagination
        messages (encrypt-feedback-ids (message/all (user-id) page per-page))]
    (layout/render "messages/index.html" (conj (set-info) {:page {:page page :max pagemax}
                                                           :news news
                                                           :messages messages}))))

(defn support-thread
  ([ticket-id]
   (let [tid (hashids/decrypt-ticket-id ticket-id)]
     (layout/render "messages/thread.html" (merge (set-info)
                                                  {:has_pub_key false
                                                   :alias (str "Support Staff")
                                                   :messages (message/all tid)})))))

(defn messages-sent []
  (layout/render "messages/sent.html" (conj (set-info) {:messages (message/sent (user-id))})))

(defn message-delete [id referer]
  (message/remove! id (user-id))
  (resp/redirect referer))

(defn messages-download [receiver-id]
  (let [user_id (user-id)
        my-name (:alias (util/current-user))
        messages (message/all (user-id) receiver-id)
        sender_name (:user_alias (first messages))
        messages (string/join "\n" (map #(str "\"" (if (= (:sender_id %) user_id) my-name (:user_alias %)) "\",\""
                                              (:created_on %) "\",\""
                                              (string/replace (:content %) #"[\"]" "\"\"") "\"") messages))]
    (-> (response messages)
        (content-type "text/plain")
        (r/header "Content-Disposition" (str "attachment;filename=" (util/format-time (java.util.Date. ) "MM-dd-yyyy") "-" sender_name "-conversation.csv")))))

(defn messages-thread
  ([receiver-id]
   (let [receiver (user/get receiver-id)]
     (layout/render "messages/thread.html" (merge (set-info)
                                                  {:has_pub_key (not (nil? (:pub_key receiver)))
                                                   :user_id receiver-id :alias (:alias receiver)
                                                   :messages (message/all (user-id) receiver-id)}))))
  ([slug & options]
   (let [message (message/add! slug (user-id) (:id slug))
         receiver (user/get (:id slug))]
     (layout/render "messages/thread.html" (merge (set-info)
                                                  {:has_pub_key (not (nil? (:pub_key receiver)))
                                                   :alias (:alias receiver)
                                                   :user_id (:id slug) :messages (message/all (user-id) (:id slug))} message)))))

(def-restricted-routes message-routes
    (GET "/message/:id/delete" {{id :id} :params {referer "referer"} :headers} (message-delete id referer))
    (GET "/messages/:id/download" [id] (messages-download id))
    (GET "/messages/support/:tid" [tid] (support-thread tid))
    (GET "/messages" [page] (messages-page page))
    (GET "/messages/sent" [] (messages-sent))
    (GET "/messages/:id" [id] (messages-thread id))
    (POST "/messages/:id" {params :params} (messages-thread params true)))
