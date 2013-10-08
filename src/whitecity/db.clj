(ns whitecity.db
  (:use korma.core
        [korma.db :only (defdb)])
  (:require [whitecity.models.schema :as schema]))

(defdb db schema/db-spec)
