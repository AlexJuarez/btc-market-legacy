(ns whitecity.models.user
  (:use [korma.db :only (defdb)]
        [whitecity.db]
        [korma.core]
        [whitecity.models.message])
  (:require 
        [whitecity.validator :as v]
        [clj-time.core :as cljtime]
        [clj-time.coerce :as tc]
        [noir.util.crypt :as warden]))

(defentity users
  (has-many messages))

;; Gets
(defn get-user [id]
  (first (select users
                 (where {:id id}))))

(defn get-by-login [login]
  (first (select users
    (where {:login login}))))

(defn get-by-alias [a]
  (first (select users
          (where {:alias a}))))

;; Mutations and Checks

(defn prep [{pass :pass :as user}]
  (assoc user :pass (warden/encrypt pass)))


(defn valid-user? [{:keys [login pass confirm] :as user}]
  (v/user-validator user))

;; Operations

(defn store! [user]
  (insert users (values user)))

(defn add! [{:keys [login pass confirm] :as user}]
  (let [check (valid-user? user)]
    (if (empty? check)
      (-> {:login login :pass pass} (prep) (store!))
      {:errors check})))

(defn last-login [id]
  (update users
          (set-fields {:last_login (tc/to-sql-date (cljtime/now))})
          (where {:id id})))

(defn login! [{:keys [login pass] :as user}]
 (let [userstore (get-by-login login)]
    (if (nil? userstore)
      (assoc user :error "Username does not exist")
    (if (and (:pass userstore) (warden/compare pass (:pass userstore)))
        (do (last-login (:id userstore)) (dissoc userstore :pass))
        (assoc user :error "Password Incorrect")))))

(defn remove! [login]
  (delete users
          (where {:login login})))
