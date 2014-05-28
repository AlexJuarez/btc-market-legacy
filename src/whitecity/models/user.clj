(ns whitecity.models.user
  (:refer-clojure :exclude [get])
  (:import (org.apache.commons.codec.binary Base64))
  (:use [whitecity.db]
        [whitecity.models.predicates]
        [korma.db :only (transaction)]
        [korma.core])
  (:require
        [whitecity.util.btc :as btc]
        [whitecity.validator :as v]
        [hiccup.util :as hc]
        [clojure.string :as s]
        [whitecity.util :as util]
        [whitecity.util.pgp :as pgp]
        [noir.session :as session]
        [whitecity.models.order :as order]
        [whitecity.models.message :as message]
        [whitecity.models.currency :as currency]
        [noir.util.crypt :as warden]))

(def ^:private salt-byte-size 24)

;; Gets
(defn get [id]
  (-> (select users
              (where {:id (util/parse-int id)}))
    first (dissoc :salt :pass)))

(defn search [query]
  (select users
          (fields :alias :fans :last_login :rating :listings :id :vendor)
          (where {:alias [ilike query] :vendor true :admin false})
          (limit 50)))

(defn get-dirty [id]
  (first (select users
                 (where {:id (util/parse-int id)}))))

(defn make-salt []
  (let [b (byte-array salt-byte-size)
        ran (java.security.SecureRandom.)]
    (do (.nextBytes ran b)
      (.toString (Base64/encodeBase64String b)))))

(defn get-by-login [login]
  (first
    (select users
      (with currency (fields [:key :currency_key] [:symbol :currency_symbol]))
      (where {:login login}))))

(defn track-login [{:keys [id last_attempted_login login_tries] :as user}]
  (if (or (= login_tries 0) (nil? last_attempted_login) (> (- (.getTime (java.util.Date.)) (.getTime last_attempted_login)) 86400000))
    (update users
            (set-fields {:last_attempted_login (raw "now()") :login_tries 1})
            (where {:id id}))
    (update users
            (set-fields {:login_tries (raw "login_tries + 1")})
            (where {:id id})))
   user)

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
  (merge (when (and (not (empty? (:pub_key user))) (nil? (pgp/get-key-ring (:pub_key user)))) {:pub_key "Invalid pgp key"})
  (v/user-update-validator user)))

(defn clean [{:keys [alias region_id auth currency_id pub_key description]}]
  {:auth (= auth "true")
   :pub_key (if (empty? pub_key) nil (clojure.string/trim pub_key))
   :currency_id (util/parse-int currency_id)
   :region_id (util/parse-int region_id)
   :description (hc/escape-html description)
   :updated_on (raw "now()")
   :alias alias})

;; Operations

(defn update! [id slug]
  (let [updates (clean slug)
        check (valid-update? updates)]
    (if (empty? check)
      (let [user (util/current-user)]
        (session/put! :user
                      (merge
                       (update users
                        (set-fields updates)
                        (where {:id id}))
                       (if-not (= (:curreny_id updates) (:currency_id user))
                                     {:currency_symbol (:symbol (currency/get (:currency_id updates)))})))
      {:errors check}))))

(defn update-btc-address! [id]
  (let [new-address (btc/newaddress id)]
    (session/put! :user (merge (util/current-user) {:wallet new-address}))
    (transaction
      (insert wallets (values {:wallet new-address :user_id id}))
      (update users (set-fields {:wallet new-address}) (where {:id id})))))

(defn update-password! [id {:keys [pass newpass confirm]}]
  (let [user (get-dirty id)]
    (if (and (not (nil? user)) (and (:pass user) (warden/compare (str pass (:salt user)) (:pass user))))
      (let [check (v/user-update-password-validator {:pass newpass :confirm confirm})]
        (if (empty? check)
          (do (update users (set-fields {:pass (warden/encrypt (str newpass (:salt user)))}) (where {:id id}))
            {})
          check))
      {:pass ["Your password is incorrect."]})))

(defn store! [user]
  (let [new-user (insert users (values user))
        user-id (:id new-user)
        wallet (btc/address user-id)]
    (insert wallets (values {:wallet wallet :user_id user-id}))
    (update users (set-fields {:wallet wallet}) (where {:id user-id}))))

(defn add! [{:keys [login pass confirm] :as user}]
  (let [check (valid-user? user)]
    (if (empty? check)
      (-> {:login login :wallet (btc/address login) :alias login :currency_id (:id (currency/find "BTC")) :pass pass :vendor true} (prep) (store!))
      {:errors check})))

(defn last-login [id session]
  (transaction
    (update users (set-fields {:session nil}) (where {:session (util/create-uuid session)}))
    (update users
            (set-fields {:login_tries 0 :last_login (raw "now()") :session (util/create-uuid session)})
            (where {:id id}))))

(defn login! [{:keys [login pass session] :as user}]
 (let [userstore (track-login (get-by-login login))]
    (if (nil? userstore)
      (assoc user :error "Username does not exist.")
      (if (> 20 (:login_tries userstore))
        (if (and (:pass userstore) (warden/compare (str pass (:salt userstore)) (:pass userstore)))
            (do (last-login (:id userstore) session) (dissoc userstore :salt :pass))
            (assoc user :error "Password Incorrect."))
        (assoc user :error "This account has been locked for failing to login too many times.")))))

(defn remove! [login]
  (delete users
          (where {:login login})))
