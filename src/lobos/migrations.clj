(ns lobos.migrations
  (:refer-clojure 
     :exclude [alter drop bigint boolean char double float time])
       (:use (lobos [migration :only [defmigration]] core schema config helpers)))

(defmigration add-currencies-table
  (up [] (create
           (tbl :currency
                (varchar :key 3 :unique :not-null)
                (varchar :name 200)
                (varchar :symbol 10))))
  (down [] (drop (table :currency))))

(defmigration add-regions-table
  (up [] (create
           (tbl :region
                (varchar :name 64))))
  (down [] (drop (table :region))))

(defmigration add-users-table
  (up [] (create
          (tbl :user
               (varchar :login 64 :unique)
               (varchar :alias 64 :unique)
               (boolean :vendor (default true))
               (boolean :admin (default true))
               (boolean :auth (default false))
               (column :session (data-type :uuid) :unique)
               (text :description)
               (text :pub_key) 
               (boolean :banned (default false))
               (varchar :pass 60)
               (varchar :salt 32);;The salt comes out to 30ish characters?
               (varchar :wallet 34 :unique)
               (integer :pin)
               (integer :transactions (default 0))
               (float :rating (default 5))
               (integer :ranking)
               (integer :fans (default 0))
               (integer :listings (default 0))
               (integer :bookmarks (default 0))
               (float :btc (default 0))
               (timestamp :last_login)
               (refer-to :currency)
               (integer :region_id [:refer :region :id :on-delete :set-null] (default 1))
               (check :login (< (length :login) 64))
               (check :alias (< (length :alias) 64))
               (check :login (> (length :login) 2))
               (check :alias (> (length :alias) 2))
               (check :btc (>= :btc 0)))))
  (down [] (drop (table :user))))

(defmigration add-user-wallets-table
  (up [] (create
           (tbl :wallet
                (varchar :wallet 34 :unique)
                (varchar :key 64)
                (timestamp :checked_on))))
  (down [] (drop (table :wallet))))

(defmigration add-messages-table
  (up [] (create
           (tbl :message
                  (boolean :read (default false))
                  (text :content)
                  (varchar :subject 100)
                  (refer-to :user)
                  (integer :sender_id [:refer :user :id :on-delete :set-null]))))
  (down [] (drop (table :messages))))

(defmigration add-images-table
  (up [] (create
           (tbl :image
                (refer-to :user)
                (varchar :name 67))))
  (down [] (drop (table :image))))

(defmigration add-exchange-rate-table
  (up [] (create
           (table :exchangerate
                (integer :from [:refer :currency :id :on-delete :set-null])
                (integer :to [:refer :currency :id :on-delete :set-null])
                (float :value)
                (timestamp :updated_on (default (now))))))
  (down [] (drop (table :exchangerate))))

(defmigration add-categories-table
  (up [] (create
           (tbl :category
                (varchar :name 30)
                (integer :count (default 0))
                (integer :lte)
                (integer :gt)
                (integer :parent))))
  (down [] (drop (table :category))))

(defmigration add-listings-table
  (up [] (create
           (tbl :listing
                (boolean :public (default false))
                (boolean :hedged (default false))
                (varchar :title 100)
                (integer :to [:refer :region :id])
                (integer :from [:refer :region :id])
                (refer-to :user)
                (refer-to :image)
                (float :price :not-null)
                (integer :quantity (default 0))
                (integer :bookmarks (default 0))
                (integer :reviews (default 0))
                (integer :sold (default 0))
                (integer :views (default 0))
                (refer-to :currency)
                (refer-to :category)
                (check :price (>= :price 0))
                (check :quantity (>= :quantity 0))
                (text :description))))
  (down [] (drop (table :listing))))

(defmigration add-postage-table
  (up [] (create
           (tbl :postage
                (refer-to :user)
                (varchar :title 100)
                (float :price :not-null)
                (refer-to :currency))))
  (down [] (drop (table :postage))))

(defmigration add-orders-table
  (up [] (create
           (tbl :order
                (float :price)
                (float :postage_price)
                (float :refund_rate (default 0))
                (varchar :postage_title 100)
                (integer :postage_currency [:refer :currency :id])
                (integer :quantity)
                (boolean :hedged (default false))
                (boolean :reviewed (default false))
                (varchar :title 100)
                (timestamp :auto_finalize)
                (text :address)
                (integer :seller_id [:refer :user :id :on-delete :set-null])
                (refer-to :currency)
                (refer-to :listing)
                (refer-to :postage)
                (refer-to :user)
                (smallint :status))))
  (down [] (drop (table :order))))

(defmigration add-escrow-table
  (up [] (create
           (tbl :escrow
                (integer :from [:refer :user :id :on-delete :set-null])
                (integer :to [:refer :user :id :on-delete :set-null])
                (float :amount)
                (check :amount (>= :amount 0))
                (boolean :hedged (default false))
                (refer-to :currency)
                (refer-to :order)
                (varchar :status 10))))
  (down [] (drop (table :escrow))))

(defmigration add-reviews-table
  (up [] (create
           (tbl :review
                (boolean :published (default false))
                (refer-to :user)
                (integer :seller_id [:refer :user :id :on-delete :set-null])
                (refer-to :listing)
                (integer :order_id :unique :not-null)
                (text :content)
                (smallint :rating :not-null (default 5))
                (boolean :shipped (default true))
                )))
  (down [] (drop (table :review))))

(defmigration add-audits-table
  (up [] (create
           (tbl :audit
                (refer-to :user)
                (varchar :role 10)
                (float :amount))))
  (down [] (drop (table :audit))))

(defmigration add-resolutions-table
  (up [] (create
           (tbl :resolution
                (integer :from [:refer :user :id :on-delete :set-null])
                (refer-to :user)
                (integer :seller_id [:refer :user :id :on-delete :set-null])
                (refer-to :order)
                (boolean :applied)
                (boolean :user_accepted (default false))
                (boolean :seller_accepted (default false))
                (varchar :action 10);; extension or refund
                (integer :value (default 0))
                (text :content)
                (check :value (>= :value 0)))))
  (down [] (drop (table :resolution))))

(defmigration add-bookmarks-table
  (up [] (create
           (tbl :bookmark
                (index :bookmark_unique_constraint [:user_id :listing_id] :unique)
                (integer :user_id [:refer :user :id :on-delete :cascade])
                (integer :listing_id [:refer :listing :id :on-delete :cascade]))))
  (down [] (drop (table :bookmark))))
                 
(defmigration add-fans-table
  (up [] (create
           (tbl :fan
                (index :fan_unique_constraint [:user_id :leader_id] :unique)
                (integer :user_id [:refer :user :id :on-delete :cascade])
                (integer :leader_id [:refer :user :id :on-delete :cascade]))))
  (down [] (drop (table :fan))))

(defmigration add-reports-table
  (up [] (create
           (tbl :report
                (index :report_unique_constraint [:object_id :user_id :type] :unique)
                (integer :object_id)
                (refer-to :user)
                (varchar :type 10 :not-null))))
  (down [] (drop (table :report))))
