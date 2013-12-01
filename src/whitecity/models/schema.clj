(ns whitecity.models.schema
  (:use [lobos.core :only (defcommand migrate)])
  (:require [noir.io :as io]
            [cheshire.core :as jr]
            [taoensso.timbre :as timbre]
            [whitecity.models.currency :as c]
            [whitecity.db :as db]
            [lobos.migration :as lm]))

(defcommand pending-migrations []
              (lm/pending-migrations db/db-spec sname))

(defn load-fixtures []
  (c/add! (jr/parse-string (slurp "resources/currencies.json") true)))

(defn actualized?
    "checks if there are no pending migrations"
    []
    (empty? (pending-migrations)))

(def actualize migrate)
