(ns whitecity.db
  (:use korma.core
        [korma.db :only (defdb)]
        [korma.core]))

(def db-spec
  {:subprotocol "postgresql"
   :subname "//localhost/whitecity"
   :user "devil"
   :password "admin"})

(defdb db db-spec)

(declare users orders messages listings postage images category currency exchange bookmarks fans reviews)

(defentity users
  (table :user)
  (has-many orders)
  (has-many reviews)
  (has-many listings)
  (has-many messages)
  (has-many images)
  (has-many postage)
  (has-many fans)
  (has-many bookmarks)
  (belongs-to currency))

(defentity sellers
  (table :user))

(defentity listings
  (table :listing)
  (belongs-to users)
  (belongs-to category)
  (belongs-to currency)
  (has-many reviews)
  (has-one images))

(defentity orders
  (table :order)
  (belongs-to sellers {:fk :seller_id})
  (belongs-to users)
  (belongs-to currency)
  (belongs-to listings)
  (belongs-to postage))

(defentity messages
  (table :message)
  (belongs-to users))

(defentity images
  (table :image)
  (belongs-to users))

(defentity postage
  (belongs-to users)
  (belongs-to currency))

(defentity category
  (has-many listings)
  (table :category))

(defentity currency
  (has-many listings)
  (table :currency))

(defentity exchange
  (table :exchangerate))

(defentity bookmarks
  (table :bookmark)
  (belongs-to users))

(defentity fans
  (table :fan)
  (belongs-to users))

(defentity reports
  (table :report)
  (belongs-to users))

(defentity reviews
  (table :review)
  (belongs-to users)
  (belongs-to listings)
  (belongs-to orders))
