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

(declare audits fee users
         withdrawals wallets senders
         feedback order-audit resolutions
         orders messages listings
         postage images category
         region currency exchange
         bookmarks fans reviews
         ships-to
         escrow modresolutions posts)

(defentity audits
  (table :audit)
  (belongs-to users))

(defentity posts
  (table :post)
  (belongs-to users))

(defentity modresolutions
  (table :modresolution)
  (belongs-to users))

(defentity wallets
  (table :wallet)
  (belongs-to users))

(defentity withdrawals
  (table :withdrawal)
  (belongs-to users))

(defentity order-audit
  (table :order-audit)
  (belongs-to users)
  (belongs-to orders))

(defentity users
  (table :user)
  (has-many orders)
  (has-many reviews)
  (has-many listings)
  (has-many messages)
  (has-many images)
  (has-many wallets)
  (has-many postage)
  (has-many fans)
  (has-many posts)
  (has-many bookmarks)
  (belongs-to currency))

(defentity sellers
  (table :user))

(defentity senders
  (table :user))

(defentity fees
  (belongs-to order)
  (table :fee))

(defentity ships-to
  (belongs-to region)
  (belongs-to listings)
  (table :ships_to))

(defentity listings
  (table :listing)
  (has-many ships-to)
  (belongs-to users)
  (belongs-to category)
  (belongs-to currency)
  (has-many reviews)
  (has-one images))

(defentity orders
  (table :order)
  (belongs-to sellers {:fk :seller_id})
  (belongs-to users)
  (has-one escrow)
  (belongs-to currency)
  (belongs-to listings)
  (belongs-to postage))

(defentity resolutions
  (table :resolution)
  (belongs-to users)
  (belongs-to sellers {:fk :seller_id})
  (belongs-to orders))

(defentity messages
  (table :message)
  (belongs-to senders {:fk :sender_id})
  (belongs-to users))

(defentity images
  (table :image)
  (belongs-to users))

(defentity postage
  (belongs-to users)
  (belongs-to currency))

(defentity escrow
  (belongs-to order))

(defentity feedback
  (belongs-to users))

(defentity category
  (has-many listings)
  (table :category))

(defentity currency
  (has-many listings)
  (table :currency))

(defentity region
  (has-many users))

(defentity exchange
  (table :exchangerate))

(defentity bookmarks
  (table :bookmark)
  (belongs-to listings)
  (belongs-to users))

(defentity fans
  (table :fan)
  (belongs-to users {:fk :leader_id}))

(defentity reports
  (table :report)
  (belongs-to users))

(defentity reviews
  (table :review)
  (belongs-to users)
  (belongs-to listings)
  (belongs-to orders))
