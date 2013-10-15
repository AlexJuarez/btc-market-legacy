(ns whitecity.models.listing
  (:use [korma.db :only (defdb)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [clj-time.core :as cljtime]
        [clj-time.coerce :as tc]
        [whitecity.util :as util]))

(defn check-field [map key]
  (if-not (nil? (key map))
    {key (util/parse-int (key map))}))

(defn prep [{:keys [title description from to public] :as listing}]
  (merge {:title title 
          :description description 
          :from from 
          :to to 
          :public (= public "true") 
          :updated_on (tc/to-sql-date (cljtime/now))} 
         (mapcat #(check-field listing %) [:quantity :image_id :price :category_id :currency_id])))

(defn get 
  ([id]
    (first (select listings
      (where {:id (util/parse-int id)}))))
  ([id user-id]
    (first (select listings
      (where {:id (util/parse-int id) :user_id user-id})))))

(defn view [id]
  (first (select listings
    (fields :id :title :currency_id :category_id :description :image_id :user_id :price :to :from)
    (with currency (fields [:name :currency_name] [:value :currency_value]))
    (with category (fields [:name :category_name]))
    (with users (fields [:login :user_login] [:alias :user_alias]))
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
      (conj {:errors check} listing))))

(defn update! [listing id user-id]
  (let [check (v/listing-validator listing)]
    (if (empty? check)
      (update listings
        (set-fields (prep listing))
       (where {:id (util/parse-int id) :user_id user-id}))
      (conj {:errors check} listing))))

(defn public
  ([] 
   (select listings
    (with users)
    (fields :title :user.alias :user_id :user.login :image_id :from :to :price :id :currency_id)
    (where {:public true})))
  ([id]
   (select listings
    (with users)
    (fields  :title :from :to :price :id :currency_id)
    (where {:public true :user_id (util/parse-int id)}))))

(defn all [id]
  (select listings
    (where {:user_id id})))
