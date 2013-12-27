(ns whitecity.models.user
  (:use [korma.db :only (defdb)]
        [whitecity.db]
        [korma.core])
  (:require 
        [whitecity.validator :as v]
        [clj-time.core :as cljtime]
        [clj-time.coerce :as tc]
        [whitecity.util :as util]
        [whitecity.models.message :as message]
        [whitecity.models.currency :as currency]
        [noir.util.crypt :as warden]))

;; Gets
(defn get [id]
  (dissoc (first (select users
           (where {:id (util/parse-int id)}))) :pass))

(defn get-by-login [login]
  (first (select users
    (with currency (fields [:key :currency_key] [:symbol :currency_symbol]))
    (where {:login login}))))

(defn get-by-alias [a]
  (first (select users
          (where {:alias a}))))

(defn get-with-pin [id pin]
  (first (select users
          (fields :login)
          (where {:id id :pin (util/parse-int pin)}))))

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
      (-> {:login login :currency_id (:id (currency/find "BTC")) :pass pass} (prep) (store!))
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
