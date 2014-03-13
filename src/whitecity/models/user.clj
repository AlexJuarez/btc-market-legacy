(ns whitecity.models.user
  (:refer-clojure :exclude [get])
  (:import (org.apache.commons.codec.binary Base64))
  (:use [whitecity.db]
        [korma.db :only (transaction)]
        [korma.core])
  (:require 
        [whitecity.validator :as v]
        [hiccup.util :as hc]
        [clojure.string :as s]
        [whitecity.util :as util]
        [noir.session :as session]
        [whitecity.models.order :as order]
        [whitecity.models.message :as message]
        [whitecity.models.currency :as currency]
        [noir.util.crypt :as warden]))

(def salt-byte-size 24)

;; Gets
(defn get [id]
  (dissoc (first (select users
           (where {:id (util/parse-int id)}))) :salt :pass))

(defn make-salt []
  (let [b (byte-array salt-byte-size)
        ran (java.security.SecureRandom.)]
    (do (.nextBytes ran b) 
      (.toString (Base64/encodeBase64String b)))))

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
  (let [salt (make-salt)]
    (assoc user :salt salt :pass (warden/encrypt (str pass salt)))))

(defn valid-user? [{:keys [login pass confirm] :as user}]
  (v/user-validator user))

(defn valid-update? [user]
  (v/user-update-validator user))

(defn clean [{:keys [alias auth currency_id pub_key description]}]
  {:auth (= auth "true")
   :pub_key pub_key
   :currency_id (util/parse-int currency_id)
   :description (hc/escape-html description)
   :updated_on (raw "now()")
   :alias alias})

;; Operations

(defn update! [id slug]
  (let [updates (clean slug)
        check (valid-update? updates)]
    (if (empty? check)
      (do 
        (session/put! :user (merge (session/get :user) updates))
        (update users
              (set-fields updates)
              (where {:id id})))
      {:errors check})))

(defn store! [user]
  (insert users (values user)))

(defn add! [{:keys [login pass confirm] :as user}]
  (let [check (valid-user? user)]
    (if (empty? check)
      (-> {:login login :alias login :currency_id (:id (currency/find "BTC")) :pass pass} (prep) (store!))
      {:errors check})))

(defn last-login [id session]
  (transaction
    (update users (set-fields {:session nil}) (where {:session (util/create-uuid session)}))
    (update users
            (set-fields {:last_login (raw "now()") :session (util/create-uuid session)})
            (where {:id id}))))

(defn login! [{:keys [login pass session] :as user}]
 (let [userstore (get-by-login login)]
    (if (nil? userstore)
      (assoc user :error "Username does not exist")
    (if (and (:pass userstore) (warden/compare (str pass (:salt userstore)) (:pass userstore)))
        (do (last-login (:id userstore) session) (dissoc userstore :salt :pass))
        (assoc user :error "Password Incorrect")))))

(defn remove! [login]
  (delete users
          (where {:login login})))
