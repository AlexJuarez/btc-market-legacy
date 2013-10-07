(ns clog.models.user
  (:use clog.models
        korma.core)
  (:require [clog.util.bcrypt :as crypt]
        [clog.util.validation :as vali]))

(defentity users)

;; Gets

(defn all []
  (select users))

(defn get-username [username]
  (first (select users
          (where {:username username}))))

;; Mutations and Checks

(defn prep [{password :password :as user}]
  (assoc user :password (crypt/encrypt password)))

(defn valid-user? [username]
  (vali/rule (empty? (get-username username))
             [:username "That username is already taken"])
  (vali/rule (vali/min-length? username 3)
             [:username "Username must be at least 3 characters."])
  (not (vali/errors? :username :password)))

(defn valid-psw? [password]
  (vali/rule (vali/min-length? password 5)
             [:password "Password must be at least 5 characters."])
  (not (vali/errors? :password)))

;; Operations

(defn store! [user]
  (insert users (values user)))

(defn add! [{:keys [username password] :as user}]
  (when (valid-user? username)
    (when (valid-psw? password)
      (-> user (prep) (store!)))))

(defn login! [{:keys [username password] :as user}]
 (let [{stored-pass :password, id :id} (get-username username)]
  (if (and stored-pass (crypt/check password stored-pass))
    {:username username, :id id}
   (vali/set-error :username "Invalid username or password"))))

(defn remove! [username]
  (delete users
          (where {:username username})))
