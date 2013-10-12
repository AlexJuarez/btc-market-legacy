(ns whitecity.db
  (:use korma.core
        [korma.db :only (defdb)]
        [korma.core])
  (:require [whitecity.models.schema :as schema]))

(defdb db schema/db-spec)

(declare users messages listings images)

(defentity users
  (has-many listings messages images))

(defentity listings
  (belongs-to users)
  (has-one images))

(defentity messages
  (belongs-to users))

(defentity images
  (belongs-to users))
