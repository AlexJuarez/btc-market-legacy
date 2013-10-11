(ns whitecity.models.listing
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [clj-time.core :as cljtime]
        [clj-time.coerce :as tc]
        [whitecity.util :as util]
        [whitecity.models.user :as user]))

(defentity listings
  (belongs-to user/users))

(defn check-field [map key]
  (if-not (nil? (key map))
    {key (util/parse-int (key map))}))

(defn prep [{:keys [title description from to public] :as listing}]
  (conj {:title title :description description :from from :to to :public (= public "true") :updated_on (tc/to-sql-date (cljtime/now))} (mapcat #(check-field listing %) [:image_id :price :category_id :currency_id])))

(defn get [id]
  (first (select listings
    (where {:id (util/parse-int id)}))))

(defn count [id]
  (:cnt (first (select listings
    (aggregate (count :*) :cnt)
    (where {:user_id id})))))

(defn remove! [id user-id]
  (delete listings
    (where {:id (util/parse-int id) :user_id user-id})))

(defn store! [listing user-id]
  (insert listings (values (assoc (prep listing) :user_id user-id))))

(defn add! [listing user-id]
  (let [check (v/listing-validator listing)]
    (if (empty? check)
      (store! listing user-id)
      {:errors check})))

(defn update! [listing id user-id]
  (let [check (v/listing-validator listing)]
    (if (empty? check)
      (update listings
        (set-fields (prep listing))
       (where {:id (util/parse-int id) :user_id user-id}))
      {:errors check})))

(defn all [id]
  (select listings
    (where {:user_id id})))
