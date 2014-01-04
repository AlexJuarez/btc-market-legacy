(ns whitecity.models.listing
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [clj-time.core :as cljtime]
        [clj-time.coerce :as tc]
        [whitecity.util :as util]))

(defn convert-post [postage]
  (map #(assoc % :price (util/convert-currency %) ) postage))

(defn convert [listings]
  (map #(assoc % :price (util/convert-currency %) :postage (convert-post (:postage %))) listings))

(defn check-field [map key]
  (if-not (nil? (key map))
    {key (util/parse-int (key map))}))

(defn prep [{:keys [title description from to public price] :as listing}]
  (merge {:title title 
          :description description 
          :from from 
          :to to 
          :public (= public "true") 
          :price (util/parse-float price)
          :updated_on (tc/to-sql-date (cljtime/now))} 
         (mapcat #(check-field listing %) [:quantity :image_id :currency_id :category_id])))

(defn get 
  ([id]
    (first (select listings
      (where {:id (util/parse-int id)}))))
  ([id user-id]
    (first (select listings
      (where {:id (util/parse-int id) :user_id user-id})))))

(defn get-in [cart]
  (if-not (nil? cart)
  (convert (select listings
    (fields [:id :lid] :hedged :quantity :title :price :category_id :currency_id :description :user.login :user.alias :user.pub_key)
          (with users
            (with postage)
          (fields :id :login :alias :pub_key))
          (where {:id [in cart]})))))

(defn view [id]
  (first (convert (select listings
    (fields :id :title :bookmarks :hedged :category_id :description :image_id :user_id :currency_id :price :to :from)
    (with category (fields [:name :category_name]))
    (with users (fields [:login :user_login] [:alias :user_alias])
          (with postage))
    (where {:id (util/parse-int id)})))))

(defn count [id]
  (:cnt (first (select listings
    (aggregate (count :*) :cnt)
    (where {:user_id id})))))

(defn remove! [id user-id]
  (if-let [listing (get id user-id)] 
    (let [category-id (:category_id listing)]
      (util/user-clear user-id)
      (transaction
        (update users (set-fields {:listings (raw "listings - 1")}) (where {:id user-id}))
        (update category (set-fields {:count (raw "count - 1")}) (where {:id category-id})) 
        (delete listings
          (where {:id (util/parse-int id) :user_id user-id}))))))

(defn store! [listing user-id]
  (let [category-id (util/parse-int (:category_id listing))]
    (util/user-clear user-id)
    (transaction 
      (update users (set-fields {:listings (raw "listings + 1")}) (where {:id user-id}))
      (update category (set-fields {:count (raw "count + 1")}) (where {:id category-id})) 
      (insert listings (values (assoc (prep listing) :user_id user-id))))))

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
   (convert (select listings
    (with users)
    (fields :title :user.alias :user_id :user.login :image_id :from :to :price :id :currency_id :category_id)
    (with currency (fields [:name :currency_name] [:key :currency_key]))
    (where {:public true :quantity [>= 0]}))))
  ([id]
   (convert (select listings
    (with users)
    (fields  :title :from :to :price :id :currency_id)
    (with currency (fields [:name :currency_name] [:key :currency_key]))
    (where {:public true :quantity [>= 0] :user_id (util/parse-int id)})))))

(defn all [id]
  (select listings
      (with currency (fields [:name :currency_name] [:key :currency_key]))
    (where {:user_id id})))
