(ns whitecity.models.schema
  (:use [lobos.core :only (defcommand migrate rollback)])
  (:require [noir.io :as io]
            [cheshire.core :as jr]
            [taoensso.timbre :as timbre]
            [whitecity.models.currency :as c]
            [whitecity.models.category :as cat]
            [whitecity.models.exchange :as e]
            [whitecity.models.region :as region]
            [whitecity.db :as db]
            [lobos.migration :as lm]))

(defcommand pending-migrations []
  (lm/pending-migrations db/db-spec sname))

(defn load-regions []
  (region/add! (jr/parse-string (slurp "resources/regions.json"))))

(defn load-currencies []
  (c/add! (distinct
           (jr/parse-string (slurp "resources/currencies_symbols.json") true))))


(defn load-fixtures []
  (when (empty? (cat/all false))
    (load-regions)
    (load-currencies)
    (e/update-from-remote)
    (cat/load-fixture)))

(defn actualized?
    "checks if there are no pending migrations"
    []
    (empty? (pending-migrations)))

(def actualize migrate)
