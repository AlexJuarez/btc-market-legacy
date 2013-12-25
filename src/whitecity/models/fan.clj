(ns whitecity.models.fan
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.util :as util]))

(defn add! [leader-id user-id]
  (insert fans (values {:leader_id (util/parse-int leader-id) :user_id user-id})))

(defn remove! [leader-id user-id]
  (delete fans
          (where {:leader_id (util/parse-int leader-id) :user_id user-id})))

(defn all [user-id]
  (select fans
          (where {:user_id user-id})))
