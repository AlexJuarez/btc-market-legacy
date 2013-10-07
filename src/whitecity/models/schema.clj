(ns whitecity.models.schema
  (:use [lobos.core :only (defcommand migrate)])
  (:require [noir.io :as io]
            [taoensso.timbre :as timbre]
            [lobos.migration :as lm]))

(def db-spec
  {:subprotocol "postgresql"
   :subname "//localhost/whitecity"
   :user "devil"
   :password "admin"})

(defcommand pending-migrations []
              (lm/pending-migrations db-spec sname))

(defn actualized?
    "checks if there are no pending migrations"
    []
    (empty? (pending-migrations)))

(def actualize migrate)
