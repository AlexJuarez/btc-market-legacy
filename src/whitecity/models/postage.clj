(ns whitecity.models.postage
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [whitecity.models.schema :as schema]
        [clj-time.core :as cljtime]
        [clj-time.coerce :as tc]
        [whitecity.util :as util]
    ))

(defn all [user-id]
  (select postage
          (where {:user_id (util/parse-int user-id)})))

(defn get 
  ([id user-id]
    (first (select postage
      (where {:id (util/parse-int id) :user_id user-id})))))

(defn remove! [id user-id]
  (delete postage
    (where {:id (util/parse-int id) :user_id user-id})))

(defn prep [{:keys [title price currency_id]}]
  {:title title 
   :price (util/parse-int price) 
   :currency_id (util/parse-int currency_id)
   :updated_on (tc/to-sql-date (cljtime/now))})

(defn store! [post user-id]
  (insert postage (values (assoc (prep post) :user_id user-id))))

(defn add! [post user-id]
  (let [check (v/postage-validator post)]
    (if (empty? check)
      (store! post user-id)
      (conj {:errors check} post))))

(defn update! [post id user-id]
  (let [check (v/postage-validator post)]
    (if (empty? check)
      (update postage
        (set-fields (prep post))
       (where {:id (util/parse-int id) :user_id user-id}))
      (conj {:errors check} post))))

(defn count [id]
  (:cnt (first (select postage
    (aggregate (count :*) :cnt)
    (where {:user_id id})))))
