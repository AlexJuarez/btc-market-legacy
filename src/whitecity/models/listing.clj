(ns whitecity.models.listing
  (:use [korma.db :only (defdb)]
        [whitecity.db])
  (:require 
        [whitecity.models.user :as user]
        [korma.core :as sql]
        [noir.util.crypt :as warden]
        [metis.core :as v]))

(sql/defentity listings
  (sql/belongs-to user/users))

(defn publish! [id]
  (sql/select listings
    (sql/where {:id id})))

(defn remove! [id]
  (sql/delete listings
    (sql/where {:id id})))

(defn add! [{:keys [title user_id image description currency_id category_id] :as listing}]
  )
