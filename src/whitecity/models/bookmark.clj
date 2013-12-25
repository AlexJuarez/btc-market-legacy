(ns whitecity.models.bookmark
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.util :as util]))

(defn add! [listing-id user-id]
  (transaction
    (update listings
            (set-fields {:bookmarks (raw "bookmarks + 1")})
            (where {:id listing-id}))
    (insert bookmarks (values {:listing_id (util/parse-int listing-id) :user_id user-id}))))

(defn remove! [listing-id user-id]
  (transaction
    (update listings
            (set-fields {:bookmarks (raw "bookmarks - 1")})
            (where {:id listing-id}))
    (delete bookmarks
            (where {:listing_id (util/parse-int listing-id) :user_id user-id}))))

(defn all [user-id]
  (select bookmarks
          (where {:user_id user-id})))
