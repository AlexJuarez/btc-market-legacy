(ns whitecity.models.user
  (:use [korma.db :only (defdb)]
        [whitecity.db]
        [korma.core])
  (:require 
        [whitecity.cache :as cache]
        [whitecity.validator :as v]
        [clj-time.core :as cljtime]
        [clj-time.coerce :as tc]
        [whitecity.util :as util]
        [noir.session :as session]
        [whitecity.models.order :as order]
        [whitecity.models.message :as message]
        [whitecity.models.currency :as currency]
        [noir.util.crypt :as warden]))

;; Gets
(defn get [id]
  (dissoc (first (select users
           (where {:id (util/parse-int id)}))) :pass))

(defn user-blob 
  ([]
   (let [id (util/user-id) 
         u (cache/get-set (str "user_" id)
            (let [user (get id)]
            (merge 
               user
               (when (:vendor user) 
                 {:sales (order/count-sales id)})
               {:errors {} 
                :messages (message/count id)
                :orders (order/count id)})))]
     (do (session/put! :user u) u)))
  ([user]
    (let [id (:id user) 
          u (cache/get-set (str "user_" id)
            (merge 
               user
               (when (:vendor user) 
                 {:sales (order/count-sales id)})
               {:errors {} 
                :messages (message/count id)
                :orders (order/count id)}))]
          (do (session/put! :user u) u))))

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

(defn valid-update? [user]
  (v/user-update-validator user))

(defn clean-alias [m]
  (let [a (:alias m) 
        user (user-blob)]
    (if (= (:alias user) a)
      (if (empty? a)
        (assoc m :alias nil)
        (dissoc m :alias))
      m)))

(defn clean [{:keys [alias auth pub_key description]}]
  (-> {;;:auth (if (= auth "true") true)
   :pub_key pub_key
   :description description
   :updated_on (tc/to-sql-date (cljtime/now))
   :alias alias}
     clean-alias
   ))

;; Operations

(defn update! [id slug]
  (let [updates (clean slug)
        check (valid-update? updates)]
    (if (empty? check)
      (do 
        (let [user-blob (merge (user-blob) updates)]
          (cache/set (str "user_" id) user-blob))
        (update users
              (set-fields updates)
              (where {:id id})))
      {:errors check})))

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
