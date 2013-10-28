(ns whitecity.db
  (:use korma.core
        [korma.db :only (defdb)]
        [korma.core])
  (:require [whitecity.models.schema :as schema]))

(defdb db schema/db-spec)

(declare users orders messages listings postage images category currency)

(defentity users
  (table :user)
  (has-many orders)
  (has-many listings)
  (has-many messages)
  (has-many images)
  (has-many postage))

(defentity sellers
  (table :user))

(defentity listings
  (table :listing)
  (belongs-to users)
  (belongs-to category)
  (belongs-to currency)
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
  (belongs-to users))

(defentity category
  (has-many listings)
  (table :category))

(defentity currency
  (has-many listings)
  (table :currency))
