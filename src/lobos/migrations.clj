(ns lobos.migrations
  (:refer-clojure 
     :exclude [alter drop bigint boolean char double float time])
       (:use (lobos [migration :only [defmigration]] core schema config helpers)))

(defmigration add-users-table
  (up [] (create
          (tbl :users
               (varchar :login 64 :unique)
               (varchar :alias 64 :unique)
               (boolean :vendor (default false))
               (boolean :admin (default false))
               (text :description)
               (text :pub_key) 
               (timestamp :last_login)
               (boolean :is_active)
               (varchar :pass 128)
               (varchar :wallet 34)
               (varchar :key 64)
               (bigint :btc (default 0))
               (check :login (> (length :login) 2))
               (check :alias (> (length :alias) 2))
              )))
  (down [] (drop (table :users))))

(defmigration add-messages-table
  (up [] (create
           (tbl :messages
                  (boolean :read (default false))
                  (text :content)
                  (varchar :subject 100)
                  (refer-to :users)
                  (integer :receiver_id [:refer :users :id :on-delete :set-null]))))
  (down [] (drop (table :messages))))

(defmigration add-images-table
  (up [] (create
           (tbl :images
                (refer-to :users)
                (varchar :path 128 :unique :not-null))))
  (down [] (drop (table :images))))

(defmigration add-currencies-table
  (up [] (create
           (tbl :currencies
                (varchar :name 30 :unique :not-null)
                (bigint :value))))
  (down [] (drop (table :currencies))))

(defmigration add-categories-table
  (up [] (create
           (tbl :categories
                (varchar :name 30)
                (integer :count)
                (integer :parent))))
  (down [] (drop (table :categories))))

(defmigration add-listings-table
  (up [] (create
           (tbl :listings
                (boolean :public (default false))
                (boolean :published (default false))
                (varchar :title 100)
                (refer-to :users)
                (refer-to :images)
                (bigint :price :not-null)
                (refer-to :currencies)
                (integer :category_id [:refer :categories :id])
                (text :description))))
  (down [] (drop (table :listings))))

(defmigration add-orders-table
  (up [] (create
           (tbl :orders
                (bigint :amount)
                (boolean :hedged)
                (refer-to :listings)
                (refer-to :users)
                (smallint :status))))
  (down [] (drop (table :orders))))

(defmigration add-reviews-table
  (up [] (create
           (tbl :reviews
                (boolean :published (default false))
                (refer-to :users)
                (refer-to :listings)
                (text :content)
                (smallint :rating :not-null (default 5))
                (boolean :shipped (default true))
                )))
  (down [] (drop (table :reviews))))

(defmigration add-audits-table
  (up [] (create
           (tbl :audits
                (refer-to :users)
                (refer-to :orders)
                (bigint :amount))))
  (down [] (drop (table :audits))))
