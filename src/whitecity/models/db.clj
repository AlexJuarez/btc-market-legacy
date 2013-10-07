(ns whitecity.models.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [whitecity.models.schema :as schema]))

(defdb db schema/db-spec)

(defentity users)

(defn create-user [user]
  (insert users
          (values user)))

(defn update-user [id moniker password]
  (update users
  (set-fields {:alias moniker
               :password password})
  (where {:id id})))

(defn get-user [id]
  (first (select users
                 (where {:id id})
                 (limit 1))))
