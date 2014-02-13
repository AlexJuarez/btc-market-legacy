(ns whitecity.models.listing
  (:use [korma.db :only (transaction)]
        [korma.core]
        [whitecity.db])
  (:require 
        [whitecity.validator :as v]
        [whitecity.models.category :as cat]
        [whitecity.util :as util]))

(defn convert-post [postage]
  (map #(assoc % :price (util/convert-currency %) ) postage))

(defn convert [listings]
  (map #(assoc % :price (util/convert-currency %) :postage (convert-post (:postage %))) listings))

(defn check-field [map key]
  (if-not (nil? (key map))
    {key (util/parse-int (key map))}))

(defn prep [{:keys [title description from to public price hedged] :as listing}]
  (merge {:title title 
          :description description
          :from from 
          :to to 
          :public (= public "true") 
          :hedged (= hedged "true")
          :price (util/parse-float price)
          :updated_on (raw "now()")} 
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
   (update listings
    (set-fields {:views (raw "views + 1")}) (where {:id (util/parse-int id)}))
  (first (convert 
   (select listings
    (fields [:id :lid] :user_id :image_id :from :to :reviews :hedged :quantity :title :price [:category.name :category_name] :category_id :currency_id :description [:user.login :user_login] [:user.alias :user_alias])
    (with users
          (with postage))
    (with category)
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
        (let [category_id (:category_id (first (select listings (fields :category_id) (where {:id (util/parse-int id) :user_id user-id}))))
              listing (prep listing)
              cat_id (:category_id listing)]
          (transaction
            (if-not (= category_id cat_id)
              (do (update category (set-fields {:count (raw "count + 1")}) (where {:id cat_id}))
              (update category (set-fields {:count (raw "count - 1")}) (where {:id category_id}))))
              (update listings
                (set-fields listing)
                (where {:id (util/parse-int id) :user_id user-id}))
              (conj {:errors check} listing))))))

(defn public
  ([] 
   (convert (select listings
    (with users)
    (fields :title :user.alias :user_id :user.login :image_id :from :to :price :id :currency_id :category_id)
    (with currency (fields [:name :currency_name] [:key :currency_key]))
    (where {:public true :quantity [>= 0]}))))
  ([cid]
   (let [c (cat/get cid) lte (:lte c) gt (:gt c)]
   (convert 
    (select listings
    (with users)
    (fields  :title :user.alias :user_id :user.login :from :to :price :id :currency_id :image_id :category_id)
    (with currency (fields [:name :currency_name] [:key :currency_key]))
    (with category)
    (where (and 
             (= :public true)
             (>= :quantity 0)
             (> :category_id gt)
             (<= :category_id lte))))))))

(defn public-for-user
  ([user-id]
   (convert 
    (select listings
    (fields  :title :from :to :price :id :currency_id :image_id :category_id)
    (with category (fields [:name :category_name]))
    (with currency (fields [:name :currency_name] [:key :currency_key]))
    (where (and 
             (= :public true)
             (>= :quantity 0)
             (= :user_id (util/parse-int user-id))))))))

(defn all [id]
  (select listings
      (with category (fields [:name :category_name]))
      (with currency (fields [:name :currency_name] [:key :currency_key]))
    (where {:user_id id})))
