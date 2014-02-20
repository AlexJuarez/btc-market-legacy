(ns whitecity.models.bookmark
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.util :as util]))

(defn get [listing-id user-id]
  (first
    (select bookmarks
            (where {:listing_id (util/parse-int listing-id) :user_id user-id}))))

(defn bookmarked? [listing-id user-id]
  (not (nil? (get listing-id user-id))))

(defn add! [listing-id user-id]
  (let [listing-id (util/parse-int listing-id)]
  (try
    (transaction
      (update users
              (set-fields {:bookmarks (raw "bookmarks + 1")})
              (where {:id user-id}))
      (update listings
              (set-fields {:bookmarks (raw "bookmarks + 1")})
              (where {:id listing-id}))
      (insert bookmarks (values {:listing_id listing-id :user_id user-id})))
    (catch Exception e
      {:errors "You have already bookmarked this"}))))

(defn remove! [listing-id user-id]
  (if-let [bookmark (get listing-id user-id)]
    (let [listing-id (util/parse-int listing-id)]
    (transaction
      (update users
              (set-fields {:bookmarks (raw "bookmarks - 1")})
              (where {:id user-id}))
      (update listings
              (set-fields {:bookmarks (raw "bookmarks - 1")})
              (where {:id listing-id}))
      (delete bookmarks
              (where {:listing_id listing-id :user_id user-id}))))))

(defn all [user-id]
  (select bookmarks
          (fields [:id :bid])
          (with listings
            (fields :user_id [:category.name :category_name] :category_id :quantity :title :id :price [:user.alias :user_alias] [:user.login :user_login] :hedged)
            (with users)
            (with category))
          (where {:user_id user-id})))
