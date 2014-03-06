(ns whitecity.models.fan
  (:refer-clojure :exclude [get])
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.util :as util]))


(defn get [leader-id user-id]
  (first
    (select fans
            (where {:leader_id (util/parse-int leader-id) :user_id user-id}))))

(defn followed? [leader-id user-id]
  (not (empty? (get leader-id user-id))))

(defn add! [leader-id user-id]
  (let [leader-id (util/parse-int leader-id)]
  (try
    (transaction
      (update users
              (set-fields {:fans (raw "fans + 1")})
              (where {:id leader-id}))
      (insert fans (values {:leader_id leader-id :user_id user-id})))
    (catch Exception e
      {:errors "You have already bookmarked this"}))))

(defn remove! [leader-id user-id]
  (if-let [fan (get leader-id user-id)]
    (let [leader-id (util/parse-int leader-id)]
    (transaction
      (update users
              (set-fields {:fans (raw "fans - 1")})
              (where {:id leader-id}))
      (delete fans
              (where {:leader_id leader-id :user_id user-id}))))))

(defn all [user-id]
  (select fans
          (with users
                (fields :login :alias :listings :rating :banned :last_login))
          (where {:user_id user-id})))
