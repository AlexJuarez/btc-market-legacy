(ns lobos.migrations
  (:refer-clojure 
     :exclude [alter drop bigint boolean char double float time])
       (:use (lobos [migration :only [defmigration]] core schema config helpers)))

(defmigration add-users-table
  (up [] (create
          (tbl :user
               (varchar :login 64 :unique)
               (varchar :alias 64 :unique)
               (boolean :vendor (default true))
               (boolean :admin (default true))
               (text :description)
               (text :pub_key) 
               (boolean :is_active)
               (varchar :pass 128)
               (varchar :wallet 34)
               (varchar :key 64)
               (integer :pin)
               (bigint :btc (default 0))
               (timestamp :last_login)
               (check :login (> (length :login) 2))
               (check :alias (> (length :alias) 2))
              )))
  (down [] (drop (table :user))))

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

(defmigration add-currencies-table
  (up [] (create
           (tbl :currency
                (varchar :name 30 :unique :not-null)
                (bigint :value))))
  (down [] (drop (table :currency))))

(defmigration add-categories-table
  (up [] (create
           (tbl :category
                (varchar :name 30)
                (integer :count)
                (integer :parent))))
  (down [] (drop (table :category))))

(defmigration add-listings-table
  (up [] (create
           (tbl :listing
                (boolean :public (default false))
                (varchar :title 100)
                (varchar :to 100)
                (varchar :from 100)
                (refer-to :user)
                (refer-to :image)
                (bigint :price :not-null)
                (integer :quantity (default 0))
                (refer-to :currency)
                (refer-to :category)
                (text :description))))
  (down [] (drop (table :listing))))

(defmigration add-postage-table
  (up [] (create
           (tbl :postage
                (refer-to :user)
                (varchar :title 100)
                (bigint :price :not-null)
                (refer-to :currency))))
  (down [] (drop (table :postage))))

(defmigration add-orders-table
  (up [] (create
           (tbl :order
                (bigint :price)
                (bigint :postage-price)
                (integer :quantity)
                (boolean :hedged)
                (varchar :title)
                (text :address)
                (refer-to :currency)
                (refer-to :listing)
                (refer-to :postage)
                (refer-to :user)
                (smallint :status))))
  (down [] (drop (table :order))))

(defmigration add-reviews-table
  (up [] (create
           (tbl :review
                (boolean :published (default false))
                (refer-to :user)
                (refer-to :listing)
                (text :content)
                (smallint :rating :not-null (default 5))
                (boolean :shipped (default true))
                )))
  (down [] (drop (table :review))))

(defmigration add-audits-table
  (up [] (create
           (tbl :audit
                (refer-to :user)
                (refer-to :order)
                (bigint :amount))))
  (down [] (drop (table :audit))))
