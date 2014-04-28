(ns whitecity.models.schema
  (:use [lobos.core :only (defcommand migrate)])
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
            (map 
              #(-> {:key (second %) :name (first %)}) 
              (jr/parse-string (slurp "resources/currencies.json") true)))))


(defn load-fixtures []
  (load-regions)
  (load-currencies)
  (e/update-from-remote)
  (cat/load-fixture))
  
(defn actualized?
    "checks if there are no pending migrations"
    []
    (empty? (pending-migrations)))

(def actualize migrate)
