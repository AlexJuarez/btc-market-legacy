(ns whitecity.models.listing
  (:refer-clojure :exclude [get get-in count])
  (:use [korma.db :only (get-connection transaction)]
        [whitecity.models.predicates]
        [korma.core]
        [whitecity.db])
  (:require
        [whitecity.validator :as v]
        [whitecity.models.category :as cat]
        [clojure.string :as string]
        [hiccup.util :as hc]
        [whitecity.util :as util]))

(defn convert-post [postage]
  (map #(assoc % :price (util/convert-currency %)) postage))

(defn convert-shipping [listing]
  (if (nil? (:to listing))
    [1]
    (vec (.getArray (:to listing)))))

(defn convert [listings]
  (map #(assoc % :to (convert-shipping %) :price (util/convert-currency %) :postage (convert-post (:postage %))) listings))

(defn add-shipping [listing]
  (assoc listing :to (convert-shipping listing)))

(defn check-field [map key]
  (if-not (nil? (key map))
    {key (util/parse-int (key map))}))

;;remove this function
(defn shipping [id]
  (select ships-to
          (where {:listing_id (util/parse-int id)})))

(defn recent-shipping [user-id]
  (select ships-to
          (fields :region_id)
          (group :region_id)
          (where {:user_id user-id})))

(defn vec->array [v]
  (.createArrayOf (.getConnection (:datasource (get-connection db))) "integer" (object-array v)))

;;TODO 26 is usd find a better way to set up this var
(defn prep [{:keys [title description from to public price hedged currency_id] :as listing}]
  (let [to (map util/parse-int to)
        to (if (some #{1} to) [1] to)]
  (merge {:title title
          :description (hc/escape-html description)
          :from (util/parse-int from)
          :to (if (empty? to) nil (vec->array (sort (distinct to))))
          :public (= public "true")
          :hedged (= hedged "true")
          :price (util/parse-float price)
          :converted_price (util/convert-price (util/parse-int currency_id) 26 (util/parse-float price))
          :updated_on (raw "now()")}
         (mapcat #(check-field listing %) [:quantity :image_id :currency_id :category_id]))))

(defn get
  ([id]
    (add-shipping
     (first (select listings
                   (with currency (fields :hedge_fee))
      (where {:id (util/parse-int id)})))))
  ([id user-id]
   (add-shipping
     (first (select listings
        (where {:id (util/parse-int id) :user_id user-id}))))))

(defn search [query]
  (convert (select listings
          (where {:public true :quantity [> 0] :title [ilike query]})
          (with category (fields [:name :category_name]))
          (limit 50))))

(defn get-in [cart]
  (if-not (nil? cart)
  (convert (select listings
    (fields [:id :lid] :hedged :quantity :title :price :category_id :currency_id :description :user.login :user.alias :user.pub_key :user_id [:user.rating :user_rating])
          (with users
            (with postage)
          (fields :id :login :alias :pub_key))
          (where {:id [in cart]})))))

;;TODO update converted_price
(defn view [id]
   (update listings
    (set-fields {:views (raw "views + 1")}) (where {:id (util/parse-int id)}))
   (first (convert
   (select listings
    (fields [:id :lid] :bookmarks :user_id :image_id :from :to :reviews :hedged :quantity :title :price [:category.name :category_name] :category_id :currency_id :description [:user.alias :user_alias])
    (with users
          (fields [:id])
          (with postage))
    (with category)
    (where {:id (util/parse-int id)})))))

(defn count
  ([id]
   (:cnt (first (select listings
                        (aggregate (count :*) :cnt)
                        (where {:user_id id})))))
  ([]
   (:cnt (first (select listings
                        (aggregate (count :*) :cnt)
                        (where {:public true :quantity [> 0]}))))))

(defn remove! [id user-id]
  (if-let [{:keys [public quantity] :as listing} (get id user-id)]
    (let [category-id (:category_id listing)]
      (util/update-session user-id)
      (transaction
        (update users (set-fields {:listings (raw "listings - 1")}) (where {:id user-id}))
        (if (and public (> quantity 0)) (update category (set-fields {:count (raw "count - 1")}) (where {:id category-id})))
        (delete listings
          (where {:id (util/parse-int id) :user_id user-id}))))))

(defn store! [{:keys [category_id public to quantity] :as listing} user-id]
  (let [category-id (util/parse-int (:category_id listing))
        listing (prep listing)
        listing-values (assoc listing :user_id user-id)]
    (util/update-session user-id)
    (let [l (transaction
      (update users (set-fields {:listings (raw "listings + 1")}) (where {:id user-id}))
      (if (and (= "true" public) (> (util/parse-int quantity) 0)) (update category (set-fields {:count (raw "count + 1")}) (where {:id category-id})))
      (insert listings (values listing-values)))]
      (insert ships-to (values (map #(hash-map :user_id user-id :region_id (util/parse-int %) :listing_id (:id l)) (map util/parse-int to))))
      l)))

(defn add! [listing user-id]
  (let [check (v/listing-validator listing)]
    (if (empty? check)
      (store! listing user-id)
      (conj {:errors check} listing))))

(defn update! [{:keys [to] :as listing} id user-id]
  (let [id (util/parse-int id)
        check (v/listing-validator listing)]
      (if (empty? check)
        (let [{category_id_old :category_id public_old :public quantity_old :quantity} (first (select listings (fields :category_id :public :quantity) (where {:id (util/parse-int id) :user_id user-id})))
              {:keys [category_id public quantity] :as listing} (prep listing)
              ships (map #(hash-map :user_id user-id :region_id (util/parse-int %) :listing_id id) (if (some #{"1"} to) [1] to))]
          (add-shipping
           (transaction
            (if (and (not (= category_id category_id_old)) public_old public (> quantity 0) (> quantity_old 0))
              (do
                (update category (set-fields {:count (raw "count + 1")}) (where {:id category_id}))
                (update category (set-fields {:count (raw "count - 1")}) (where {:id category_id_old})))
              (if (or (and public (> quantity 0) (not public_old))
                      (and public (> quantity 0) (<= quantity_old 0)))
                (update category (set-fields {:count (raw "count + 1")}) (where {:id category_id}))
                (if (or (and (not public) public_old (> quantity_old 0))
                        (and (<= quantity 0) public_old (> quantity_old 0)))
                  (update category (set-fields {:count (raw "count - 1")}) (where {:id category_id})))))
            (delete ships-to (where {:listing_id id}))
            (insert ships-to (values ships))
            (update listings
                    (set-fields listing)
                    (where {:id id :user_id user-id})))))
          (conj {:errors check} listing))))

(defn- sortby [query page per-page {:keys [sort_by ships_to ships_from]}]
  (let [query (-> query
                  (offset (* (- page 1) per-page))
                  (limit per-page))
        query
        (cond
          (= sort_by "highest") (-> query (order :converted_price :desc))
          (= sort_by "lowest") (-> query (order :converted_price :asc))
          (= sort_by "title") (-> query (order :title :asc))
          (= sort_by "newest") (-> query (order :created_on :desc))
          :else (-> query (order :sold :desc)))
          query (if (and (not (= (:region_id (util/current-user)) 1)) ships_to)
                  (-> query (where (raw (str (:region_id (util/current-user)) " = any (\"listing\".\"to\")")))) query)
          query (if (and (not (= (:region_id (util/current-user)) 1)) ships_from)
                  (-> query (where {:from (:region_id (util/current-user))})) query)]
    query))

(defn- gen-public-query []
  (->
   (select* listings)
   (with users)
   (fields :title :user.alias :user.rating :user_id :user.login :image_id :from :to :price :id :currency_id :category_id)
   (with currency (fields [:name :currency_name] [:key :currency_key]))))

(defn public
  ([cid page per-page options]
   (let [c (cat/get cid) lte (:lte c) gt (:gt c)]
   (convert
    (let [query (->
      (gen-public-query)
      (with category)
      (where (and
               (= :public true)
               (> :quantity 0)
               (> :category_id gt)
               (<= :category_id lte))))]
      (-> (sortby query page per-page options) select))))))


(defn public-for-user
  ([user-id page per-page]
   (convert
    (select listings
            (fields  :title :from :to :price :id :currency_id :image_id :category_id)
            (with category (fields [:name :category_name]))
            (with currency (fields [:name :currency_name] [:key :currency_key]))
            (where (and
                    (= :public true)
                    (> :quantity 0)
                    (= :user_id (util/parse-int user-id))))
            (order :title :asc)
            (offset (* (- page 1) per-page))
            (limit per-page)))))

(defn all
  ;;This is for the vendor listings page
  ([id page per-page]
   (map add-shipping
        (select listings
           (with category (fields [:name :category_name]))
           (with currency (fields [:name :currency_name] [:symbol :currency_symbol] [:key :currency_key]))
           (where {:user_id id})
           (offset (* (- page 1) per-page))
           (limit per-page)
           (order :title :asc))))
  ;;This is for the grams api
  ([page per-page]
   (map
    add-shipping
    (select listings
           (fields [:id :hash] [:title :name] [:created_on :item_create_time] [:updated_on :item_update_time] :description :price :currency_id :from :to :image_id :category_id :rating [:sold :item_tran_count])
           (with users (fields :id [:rating :vendor_rating] [:transactions :vendor_tran_count] [:pub_key :vendor_pgp] [:pub_key_id :vendor_fingerprint] [:alias :vendor_name])) ;;for the grams market api
           (with category (fields [:name :category]))
           (limit per-page)
           (offset (* (- page 1) per-page))
           (where {:public true :quantity [> 0]})
           (order :updated_on :asc)))))
